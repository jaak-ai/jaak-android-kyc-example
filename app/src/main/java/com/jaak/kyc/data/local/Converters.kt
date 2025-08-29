package com.jaak.kyc.data.local

import androidx.room.TypeConverter
import com.jaak.kyc.data.local.entity.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room type converters for enums
 */
class Converters {
    
    @TypeConverter
    fun fromKycProcessStatus(status: KycProcessStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toKycProcessStatus(status: String): KycProcessStatus {
        return KycProcessStatus.valueOf(status)
    }
    
    @TypeConverter
    fun fromServiceStatus(status: ServiceStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toServiceStatus(status: String): ServiceStatus {
        return ServiceStatus.valueOf(status)
    }
    
    @TypeConverter
    fun fromKycServiceType(serviceType: KycServiceType): String {
        return serviceType.name
    }
    
    @TypeConverter
    fun toKycServiceType(serviceType: String): KycServiceType {
        return KycServiceType.valueOf(serviceType)
    }
    
    @TypeConverter
    fun fromKycServiceTypeList(serviceTypes: List<KycServiceType>): String {
        return Gson().toJson(serviceTypes.map { it.name })
    }
    
    @TypeConverter
    fun toKycServiceTypeList(serviceTypesJson: String): List<KycServiceType> {
        val type = object : TypeToken<List<String>>() {}.type
        val serviceTypeNames: List<String> = Gson().fromJson(serviceTypesJson, type)
        return serviceTypeNames.map { KycServiceType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromTokenSource(tokenSource: TokenSource): String {
        return tokenSource.name
    }
    
    @TypeConverter
    fun toTokenSource(tokenSource: String): TokenSource {
        return TokenSource.valueOf(tokenSource)
    }
    
    @TypeConverter
    fun fromErrorCategory(category: ErrorCategory): String {
        return category.name
    }
    
    @TypeConverter
    fun toErrorCategory(category: String): ErrorCategory {
        return ErrorCategory.valueOf(category)
    }
    
    @TypeConverter
    fun fromErrorSeverity(severity: ErrorSeverity): String {
        return severity.name
    }
    
    @TypeConverter
    fun toErrorSeverity(severity: String): ErrorSeverity {
        return ErrorSeverity.valueOf(severity)
    }
    
    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return Gson().toJson(list)
    }
    
    @TypeConverter
    fun toStringList(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(json, type) ?: emptyList()
    }
}