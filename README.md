# 「個案暨輔導員管理系統」後台API
###### 資料庫連線
```properties
# LMS
url = jdbc:log4jdbc:sqlserver://192.168.1.100:1433;databaseName=CaseManagementT
```
```

## 共通規則
##### Model
+ 資料庫欄位格式對應
  + 日期 datetime2(3)  => 後台Java 用 LocalDataTime
  + 小數點 decimal()   => 後台Java 用 BigDecimal
##### Payload
+ 必須輸入欄位檢核
+ 日期資料用西元年格式，民國年格式由前端轉換
##### Controller
+ Url 依 程式代號命名，詳細依 SD 文件描述
##### Swagger
+ Controller
+ Dto
+ Payload

## 程式命名規則
##### Package
> 依模組
>> com.hn2.** => com.hn2.mail 郵件

> 依系統
>> com.hn2.** => com.hn2.md 醫材

##### Payload
> 依模組
>> ex: Flow...Payload

> 依 程式代號
>> ex: Mdreg1000...Payload

##### Contoller
> 依模組
>> ex: Flow...Controller

> 依 程式代號
>> ex: Mdreg1000...Controller

##### Service
> 依模組
>> ex: Flow...Service

> 依 程式代號
>> ex: Mdreg1000...Service

> 依 Model
> ex: RegBaseService (Model:RegBase)

##### Repository
> 依 Model
>> ex: RegBaseRepository (Model:RegBase)

> 依 程式代號 , 客製 native 查詢
>> Mdreg1000CustormerRepository

##### DTO
> 依模組
>> Flow...Dto
> 依 程式代號
>> Mdreg1000Dto

## 程式包說明
+ hnsquare-app-api
  + **產製 war**
+ hnsquare-base
  + 基礎元件
+ hnsquare-config
  + 設定元件
+ hnsquare-utility
  + 工具元件
+ hnsquare-cms
  + 業務模組  