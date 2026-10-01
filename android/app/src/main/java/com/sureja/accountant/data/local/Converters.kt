package com.sureja.accountant.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun payment(value: PaymentMethod) = value.name
    @TypeConverter fun payment(value: String) = PaymentMethod.valueOf(value)
    @TypeConverter fun source(value: TransactionSource) = value.name
    @TypeConverter fun source(value: String) = TransactionSource.valueOf(value)
    @TypeConverter fun status(value: TransactionStatus) = value.name
    @TypeConverter fun status(value: String) = TransactionStatus.valueOf(value)
    @TypeConverter fun sync(value: SyncStatus) = value.name
    @TypeConverter fun sync(value: String) = SyncStatus.valueOf(value)
}

