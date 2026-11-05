package com.example.let_us_go.data.model

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.io.IOException

/**
 * User Moshi Adapter（Factory 方式，优先级最高）
 */
object UserAdapterFactory : JsonAdapter.Factory {

    override fun create(type: java.lang.reflect.Type, annotations: Set<Annotation>, moshi: com.squareup.moshi.Moshi): JsonAdapter<*>? {
        if (type != User::class.java) return null
        Log.d("UserAdapter", "UserAdapterFactory.create() 被调用，创建自定义适配器")
        return object : JsonAdapter<User>() {
            @Throws(IOException::class)
            override fun fromJson(reader: JsonReader): User? {
                Log.d("UserAdapter", "UserAdapter.fromJson() 开始解析 User")
                var id: Long = 0L
                var username: String = ""
                var password: String? = null
                var phoneNumber: String? = null
                var email: String? = null
                var nickname: String? = null
                var level: Int = 1
                var points: Int = 0
                var scenicName: String? = null
                var avatar: String? = null
                var avatarText: String? = null

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "userId"       -> id = reader.nextLong()
                        "username"     -> username = reader.nextString()
                        "password"     -> password = readNullString(reader)
                        "phoneNumber"  -> phoneNumber = readNullString(reader)
                        "email"        -> email = readNullString(reader)
                        "nickname"     -> nickname = readNullString(reader)
                        "level"        -> level = reader.nextInt()
                        "points"       -> points = reader.nextInt()
                        "scenicName"   -> scenicName = readNullString(reader)
                        "avatar"       -> avatar = readNullString(reader)
                        "avatarText"   -> avatarText = readNullString(reader)
                        else           -> reader.skipValue()
                    }
                }
                reader.endObject()

                val user = User(
                    userId = id,
                    username = username,
                    password = password,
                    phoneNumber = phoneNumber,
                    email = email,
                    nickname = nickname,
                    level = level,
                    points = points,
                    scenicName = scenicName,
                    avatar = avatar,
                    avatarText = avatarText
                )
                Log.d("UserAdapter", "解析完成: userId=${user.userId}, username=${user.username}")
                return user
            }

            override fun toJson(writer: JsonWriter, value: User?) {
                if (value == null) { writer.nullValue(); return }
                writer.beginObject()
                writer.name("userId").value(value.userId)
                writer.name("username").value(value.username)
                value.password?.let     { writer.name("password").value(it) }
                value.phoneNumber?.let  { writer.name("phoneNumber").value(it) }
                value.email?.let        { writer.name("email").value(it) }
                value.nickname?.let     { writer.name("nickname").value(it) }
                writer.name("level").value(value.level)
                writer.name("points").value(value.points)
                value.scenicName?.let   { writer.name("scenicName").value(it) }
                value.avatar?.let       { writer.name("avatar").value(it) }
                value.avatarText?.let   { writer.name("avatarText").value(it) }
                writer.endObject()
            }
        }
    }

    private fun readNullString(reader: JsonReader): String? {
        return if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Unit>(); null
        } else {
            reader.nextString()
        }
    }
}
