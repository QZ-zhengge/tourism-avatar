"""
地图打卡路由（前端地图点亮功能）

接口：
- POST /checkin/                添加一次省份打卡（幂等：重复打卡返回已点亮）
- GET  /checkin/user/{user_id} 获取某用户已打卡的所有省份编号列表
- GET  /checkin/provinces      获取标准省份编号列表（供前端渲染地图底图）
"""
import logging

from fastapi import APIRouter, Depends, Body
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError

from database import get_db
from models.checkin import ProvinceCode, UserCheckin
from schemas.checkin import CheckinAddRequest

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/checkin", tags=["地图打卡"])


@router.post("/", summary="添加一次省份打卡（幂等）")
async def add_checkin(data: CheckinAddRequest = Body(...), db: Session = Depends(get_db)):
    """
    前端在地图上点亮某省份时调用。
    - user_id：必填，用户ID
    - province_code：必填，标准省份编号（需存在于 province_codes 表）
    重复打卡同一省份时返回 newly_added=false，不报错（幂等）。
    """
    province = db.query(ProvinceCode).filter(
        ProvinceCode.province_code == data.province_code
    ).first()
    if not province:
        return {
            "code": 400,
            "msg": f"无效的省份编号: {data.province_code}",
            "data": None,
        }

    existing = db.query(UserCheckin).filter(
        UserCheckin.user_id == data.user_id,
        UserCheckin.province_code == data.province_code,
    ).first()
    if existing:
        return {
            "code": 200,
            "msg": "该省份已点亮",
            "data": {
                "province_code": province.province_code,
                "province_name": province.province_name,
                "newly_added": False,
            },
        }

    rec = UserCheckin(user_id=data.user_id, province_code=data.province_code)
    db.add(rec)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        return {
            "code": 200,
            "msg": "该省份已点亮",
            "data": {
                "province_code": province.province_code,
                "province_name": province.province_name,
                "newly_added": False,
            },
        }
    db.refresh(rec)
    logger.info(f"[打卡] 用户 {data.user_id} 点亮省份 {data.province_code}")

    return {
        "code": 200,
        "msg": "打卡成功",
        "data": {
            "province_code": province.province_code,
            "province_name": province.province_name,
            "newly_added": True,
        },
    }


@router.get("/user/{user_id}", summary="获取用户已打卡省份编号列表")
async def user_checkins(user_id: int, db: Session = Depends(get_db)):
    """
    返回该用户已点亮的所有省份编号（按首次打卡时间升序）。
    前端据此在地图上点亮对应省份。
    """
    rows = (
        db.query(UserCheckin.province_code)
        .filter(UserCheckin.user_id == user_id)
        .order_by(UserCheckin.created_at.asc())
        .all()
    )
    codes = [r[0] for r in rows]
    return {"code": 200, "msg": "ok", "data": codes}


@router.get("/provinces", summary="获取标准省份编号列表")
async def province_list(db: Session = Depends(get_db)):
    """
    返回全部标准省份编号与名称，供前端渲染地图底图 / 校验编号合法性。
    """
    rows = db.query(ProvinceCode).order_by(ProvinceCode.id.asc()).all()
    data = [
        {"province_code": r.province_code, "province_name": r.province_name}
        for r in rows
    ]
    return {"code": 200, "msg": "ok", "data": data}
