package com.example.let_us_go.data.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.lang.reflect.Type

/**
 * 容错 Moshi 适配器
 *
 * 后端文档与实测返回在「经纬度」「可见性」等字段上类型不完全一致
 * （文档写 string / int，个别接口返回数字 / 字符串）。为避免 Moshi 因
 * 类型不符抛 JsonDataException 导致整页解析崩溃，这里用带 @JsonQualifier
 * 的适配器把标量互转：
 *  - [LenientString]：STRING → 原样；NUMBER → 转字符串；NULL → null
 *  - [LenientInt]   ：NUMBER → 原样；STRING → 尝试解析为 int；NULL → null
 */
object MoshiLenientAdapters {

    @Retention(AnnotationRetention.RUNTIME)
    @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.FIELD,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.TYPE
    )
    @JsonQualifier
    annotation class LenientString

    @Retention(AnnotationRetention.RUNTIME)
    @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.FIELD,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.TYPE
    )
    @JsonQualifier
    annotation class LenientInt

    class LenientStringAdapter {
        @FromJson
        @LenientString
        fun fromJson(reader: JsonReader): String? {
            return when (reader.peek()) {
                JsonReader.Token.NULL -> {
                    reader.nextNull<Unit>()
                    null
                }
                JsonReader.Token.NUMBER -> reader.nextDouble().toString()
                JsonReader.Token.STRING -> reader.nextString()
                else -> {
                    reader.skipValue()
                    null
                }
            }
        }

        @ToJson
        fun toJson(writer: JsonWriter, @LenientString value: String?) {
            if (value == null) writer.nullValue() else writer.value(value)
        }
    }

    class LenientIntAdapter {
        @FromJson
        @LenientInt
        fun fromJson(reader: JsonReader): Int {
            return when (reader.peek()) {
                JsonReader.Token.NULL -> {
                    reader.nextNull<Unit>()
                    0
                }
                JsonReader.Token.NUMBER -> reader.nextInt()
                JsonReader.Token.STRING -> reader.nextString().toIntOrNull() ?: 0
                else -> {
                    reader.skipValue()
                    0
                }
            }
        }

        @ToJson
        fun toJson(writer: JsonWriter, @LenientInt value: Int) {
            writer.value(value)
        }
    }
}
