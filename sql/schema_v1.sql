-- ============================================================
-- 企业员工考勤管理系统 · 从零建库基线（结构 + 系统字典）
--
-- 来源：当前运行库的 mysqldump 结构 + 字典表数据，已剔除全部账号、
--       员工档案、考勤流水、审批流水、审计日志与任何真实口令。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p < sql/schema_v1.sql
-- 说明：本文件已包含 V2/V3/V4 全部表结构与字典；从零部署只需执行本文件。
--       sql/schema_v2.sql / schema_v3.sql / schema_v4.sql 是历史增量升级
--       脚本（幂等），仅用于将旧版本库升级，新库无需执行。
-- 首次启动后由 DataInitializer 自动创建唯一超级管理员 superadmin，
-- 其初始口令取自 application-local.yml / 环境变量 SUPER_ADMIN_PASSWORD。
-- ============================================================
SET NAMES utf8mb4;

-- MySQL dump 10.13  Distrib 8.4.9, for Win64 (x86_64)
--
-- Host: localhost    Database: attendance_db
-- ------------------------------------------------------
-- Server version	8.4.9

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `attendance_db`
--

/*!40000 DROP DATABASE IF EXISTS `attendance_db`*/;


CREATE DATABASE IF NOT EXISTS `attendance_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `attendance_db`;

--
-- Table structure for table `approval_record`
--

DROP TABLE IF EXISTS `approval_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(30) NOT NULL COMMENT 'LEAVE/OVERTIME/TRIP/MAKEUP/APPEAL',
  `biz_id` bigint NOT NULL,
  `step_no` int NOT NULL DEFAULT '1',
  `approver_id` bigint DEFAULT NULL,
  `approver_name` varchar(50) DEFAULT NULL,
  `action` varchar(20) NOT NULL COMMENT 'SUBMIT/APPROVE/REJECT/CANCEL/TRANSFER',
  `remark` varchar(300) DEFAULT NULL,
  `approve_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `approval_record`
--

LOCK TABLES `approval_record` WRITE;
/*!40000 ALTER TABLE `approval_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `approval_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_appeal`
--

DROP TABLE IF EXISTS `attendance_appeal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_appeal` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL,
  `record_id` bigint DEFAULT NULL COMMENT '关联考勤记录',
  `appeal_date` date NOT NULL,
  `exception_type` varchar(30) NOT NULL COMMENT 'LATE/EARLY_LEAVE/ABSENT/MISSING_OUT',
  `reason` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `reply` varchar(300) DEFAULT NULL,
  `approver_id` bigint DEFAULT NULL,
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考勤异常申诉表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_appeal`
--

LOCK TABLES `attendance_appeal` WRITE;
/*!40000 ALTER TABLE `attendance_appeal` DISABLE KEYS */;
/*!40000 ALTER TABLE `attendance_appeal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_group`
--

DROP TABLE IF EXISTS `attendance_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '考勤组名称',
  `group_type` varchar(20) NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED固定班制/SCHEDULE排班制/FLEX自由工时',
  `department_id` bigint DEFAULT NULL COMMENT '适用部门',
  `default_shift_id` bigint DEFAULT NULL COMMENT '默认班次',
  `workdays` varchar(20) NOT NULL DEFAULT '2,3,4,5,6' COMMENT '工作日(1=周日..7=周六,MySQL DAYOFWEEK)',
  `allow_ip` varchar(100) DEFAULT NULL COMMENT '允许打卡IP段,逗号分隔',
  `wifi_ssid` varchar(100) DEFAULT NULL COMMENT '允许WiFi SSID',
  `punch_ahead_min` int NOT NULL DEFAULT '120' COMMENT '最早可提前打卡分钟',
  `remark` varchar(200) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考勤组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_group`
--

LOCK TABLES `attendance_group` WRITE;
/*!40000 ALTER TABLE `attendance_group` DISABLE KEYS */;
INSERT INTO `attendance_group` VALUES (1,'总部职能考勤组','FIXED',NULL,1,'2,3,4,5,6','127.0.0.1,192.168.1.*','Corp-Office',120,'固定班制，周一至周五 09:00-18:00',1,'2026-11-21 15:27:58'),(2,'研发弹性考勤组','FLEX',1,1,'2,3,4,5,6','127.0.0.1,10.0.*','Corp-RD',120,'弹性工时，按日统计实际工时',1,'2026-11-21 15:27:58'),(3,'客服排班考勤组','SCHEDULE',4,2,'2,3,4,5,6,7','127.0.0.1','Corp-CS',120,'排班制，含周末轮班',1,'2026-11-21 15:27:58');
/*!40000 ALTER TABLE `attendance_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_group_location`
--

DROP TABLE IF EXISTS `attendance_group_location`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_group_location` (
  `group_id` bigint NOT NULL,
  `location_id` bigint NOT NULL,
  PRIMARY KEY (`group_id`,`location_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考勤组允许打卡地点关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_group_location`
--

LOCK TABLES `attendance_group_location` WRITE;
/*!40000 ALTER TABLE `attendance_group_location` DISABLE KEYS */;
/*!40000 ALTER TABLE `attendance_group_location` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_record`
--

DROP TABLE IF EXISTS `attendance_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `attendance_date` date NOT NULL COMMENT '考勤日期',
  `check_in_time` datetime DEFAULT NULL COMMENT '签到时间',
  `check_out_time` datetime DEFAULT NULL COMMENT '签退时间',
  `status` varchar(20) DEFAULT 'NORMAL' COMMENT '状态: NORMAL/LATE/EARLY_LEAVE/ABSENT',
  `shift_id` bigint DEFAULT NULL COMMENT '当日班次',
  `check_in_ip` varchar(50) DEFAULT NULL COMMENT '签到IP',
  `check_out_ip` varchar(50) DEFAULT NULL COMMENT '签退IP',
  `check_in_device` varchar(50) DEFAULT NULL COMMENT '打卡终端',
  `source` varchar(20) NOT NULL DEFAULT 'WEB' COMMENT 'WEB/APP/DEVICE/MAKEUP',
  `late_minutes` int NOT NULL DEFAULT '0' COMMENT '迟到分钟',
  `early_minutes` int NOT NULL DEFAULT '0' COMMENT '早退分钟',
  `work_hours` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '实际工时',
  `is_makeup` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否补卡生成',
  `remark` varchar(300) DEFAULT NULL,
  `location_id` bigint DEFAULT NULL COMMENT '打卡地点ID',
  `punch_lat` decimal(10,6) DEFAULT NULL COMMENT '打卡纬度',
  `punch_lng` decimal(10,6) DEFAULT NULL COMMENT '打卡经度',
  `distance_m` int DEFAULT NULL COMMENT '距地点中心距离(米)',
  `punch_address` varchar(200) DEFAULT NULL COMMENT '打卡地点名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_date` (`employee_id`,`attendance_date`),
  KEY `idx_att_date` (`attendance_date`),
  CONSTRAINT `fk_record_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1744 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考勤记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_record`
--

LOCK TABLES `attendance_record` WRITE;
/*!40000 ALTER TABLE `attendance_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `attendance_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_summary`
--

DROP TABLE IF EXISTS `attendance_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL,
  `ym` varchar(7) NOT NULL COMMENT '统计月份 yyyy-MM',
  `expected_days` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '应出勤天数',
  `actual_days` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '实际出勤天数',
  `late_cnt` int NOT NULL DEFAULT '0',
  `early_cnt` int NOT NULL DEFAULT '0',
  `absent_cnt` int NOT NULL DEFAULT '0',
  `leave_days` decimal(5,1) NOT NULL DEFAULT '0.0',
  `overtime_hours` decimal(6,1) NOT NULL DEFAULT '0.0',
  `makeup_cnt` int NOT NULL DEFAULT '0',
  `work_hours` decimal(7,1) NOT NULL DEFAULT '0.0',
  `late_minutes` int NOT NULL DEFAULT '0',
  `locked` tinyint(1) NOT NULL DEFAULT '0' COMMENT '月度关账锁定',
  `generate_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_summary` (`employee_id`,`ym`)
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='月度考勤汇总表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_summary`
--

LOCK TABLES `attendance_summary` WRITE;
/*!40000 ALTER TABLE `attendance_summary` DISABLE KEYS */;
/*!40000 ALTER TABLE `attendance_summary` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `business_trip`
--

DROP TABLE IF EXISTS `business_trip`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `business_trip` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL,
  `destination` varchar(100) NOT NULL COMMENT '出差地点',
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `days` decimal(4,1) NOT NULL COMMENT '出差天数',
  `reason` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `approver_id` bigint DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `approve_remark` varchar(300) DEFAULT NULL,
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出差申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `business_trip`
--

LOCK TABLES `business_trip` WRITE;
/*!40000 ALTER TABLE `business_trip` DISABLE KEYS */;
/*!40000 ALTER TABLE `business_trip` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `department` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) NOT NULL COMMENT '部门名称',
  `description` varchar(200) DEFAULT NULL COMMENT '部门描述',
  `code` varchar(30) DEFAULT NULL COMMENT '部门编码',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '上级部门',
  `leader_id` bigint DEFAULT NULL COMMENT '部门负责人',
  `sort_no` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `department`
--

LOCK TABLES `department` WRITE;
/*!40000 ALTER TABLE `department` DISABLE KEYS */;
INSERT INTO `department` VALUES (1,'技术部','负责系统开发与技术维护',NULL,0,NULL,0),(2,'人事部','负责员工招聘与人事管理',NULL,0,NULL,0),(3,'财务部','负责公司财务管理',NULL,0,NULL,0),(4,'市场部','负责市场推广与客户拓展',NULL,0,NULL,0);
/*!40000 ALTER TABLE `department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employee`
--

DROP TABLE IF EXISTS `employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_no` varchar(20) NOT NULL COMMENT '工号',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `department_id` bigint DEFAULT NULL COMMENT '部门ID',
  `group_id` bigint DEFAULT NULL COMMENT '考勤组ID',
  `shift_id` bigint DEFAULT NULL COMMENT '默认班次ID',
  `position` varchar(50) DEFAULT NULL COMMENT '岗位',
  `hire_date` date DEFAULT NULL COMMENT '入职日期',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/LEAVE',
  `employment_type` varchar(20) NOT NULL DEFAULT 'REGULAR' COMMENT 'PROBATION试用/REGULAR正式/INTERN实习',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `id_card` varchar(30) DEFAULT NULL COMMENT '身份证号',
  `education` varchar(30) DEFAULT NULL COMMENT '学历',
  `address` varchar(200) DEFAULT NULL COMMENT '住址',
  `emergency_contact` varchar(50) DEFAULT NULL COMMENT '紧急联系人',
  `emergency_phone` varchar(20) DEFAULT NULL COMMENT '紧急联系电话',
  `leave_date` date DEFAULT NULL COMMENT '离职日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `employee_no` (`employee_no`),
  KEY `fk_emp_dept` (`department_id`),
  KEY `idx_emp_status` (`status`),
  CONSTRAINT `fk_emp_dept` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employee`
--

LOCK TABLES `employee` WRITE;
/*!40000 ALTER TABLE `employee` DISABLE KEYS */;
/*!40000 ALTER TABLE `employee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `holiday_calendar`
--

DROP TABLE IF EXISTS `holiday_calendar`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `holiday_calendar` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cal_date` date NOT NULL COMMENT '日历日期',
  `day_type` varchar(20) NOT NULL COMMENT 'WORKDAY调休上班/REST休息日/HOLIDAY法定节假日',
  `name` varchar(50) DEFAULT NULL COMMENT '节日或调整说明',
  `remark` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `cal_date` (`cal_date`)
) ENGINE=InnoDB AUTO_INCREMENT=135 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作日历表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `holiday_calendar`
--

LOCK TABLES `holiday_calendar` WRITE;
/*!40000 ALTER TABLE `holiday_calendar` DISABLE KEYS */;
INSERT INTO `holiday_calendar` VALUES (1,'2026-01-03','REST','周末休息','系统按周末自动生成'),(2,'2026-01-04','REST','周末休息','系统按周末自动生成'),(3,'2026-01-10','REST','周末休息','系统按周末自动生成'),(4,'2026-01-11','REST','周末休息','系统按周末自动生成'),(5,'2026-01-17','REST','周末休息','系统按周末自动生成'),(6,'2026-01-18','REST','周末休息','系统按周末自动生成'),(7,'2026-01-24','REST','周末休息','系统按周末自动生成'),(8,'2026-01-25','REST','周末休息','系统按周末自动生成'),(9,'2026-01-31','REST','周末休息','系统按周末自动生成'),(10,'2026-02-01','REST','周末休息','系统按周末自动生成'),(11,'2026-02-07','REST','周末休息','系统按周末自动生成'),(12,'2026-02-08','REST','周末休息','系统按周末自动生成'),(13,'2026-02-14','REST','周末休息','系统按周末自动生成'),(14,'2026-02-15','REST','周末休息','系统按周末自动生成'),(15,'2026-02-21','REST','周末休息','系统按周末自动生成'),(16,'2026-02-22','REST','周末休息','系统按周末自动生成'),(17,'2026-02-28','REST','周末休息','系统按周末自动生成'),(18,'2026-03-01','REST','周末休息','系统按周末自动生成'),(19,'2026-03-07','REST','周末休息','系统按周末自动生成'),(20,'2026-03-08','REST','周末休息','系统按周末自动生成'),(21,'2026-03-14','REST','周末休息','系统按周末自动生成'),(22,'2026-03-15','REST','周末休息','系统按周末自动生成'),(23,'2026-03-21','REST','周末休息','系统按周末自动生成'),(24,'2026-03-22','REST','周末休息','系统按周末自动生成'),(25,'2026-03-28','REST','周末休息','系统按周末自动生成'),(26,'2026-03-29','REST','周末休息','系统按周末自动生成'),(27,'2026-04-04','REST','周末休息','系统按周末自动生成'),(28,'2026-04-05','REST','周末休息','系统按周末自动生成'),(29,'2026-04-11','REST','周末休息','系统按周末自动生成'),(30,'2026-04-12','REST','周末休息','系统按周末自动生成'),(31,'2026-04-18','REST','周末休息','系统按周末自动生成'),(32,'2026-04-19','REST','周末休息','系统按周末自动生成'),(33,'2026-04-25','REST','周末休息','系统按周末自动生成'),(34,'2026-04-26','REST','周末休息','系统按周末自动生成'),(35,'2026-05-02','REST','周末休息','系统按周末自动生成'),(36,'2026-05-03','REST','周末休息','系统按周末自动生成'),(37,'2026-05-09','REST','周末休息','系统按周末自动生成'),(38,'2026-05-10','REST','周末休息','系统按周末自动生成'),(39,'2026-05-16','REST','周末休息','系统按周末自动生成'),(40,'2026-05-17','REST','周末休息','系统按周末自动生成'),(41,'2026-05-23','REST','周末休息','系统按周末自动生成'),(42,'2026-05-24','REST','周末休息','系统按周末自动生成'),(43,'2026-05-30','REST','周末休息','系统按周末自动生成'),(44,'2026-05-31','REST','周末休息','系统按周末自动生成'),(45,'2026-06-06','REST','周末休息','系统按周末自动生成'),(46,'2026-06-07','REST','周末休息','系统按周末自动生成'),(47,'2026-06-13','REST','周末休息','系统按周末自动生成'),(48,'2026-06-14','REST','周末休息','系统按周末自动生成'),(49,'2026-06-20','REST','周末休息','系统按周末自动生成'),(50,'2026-06-21','REST','周末休息','系统按周末自动生成'),(51,'2026-06-27','REST','周末休息','系统按周末自动生成'),(52,'2026-06-28','REST','周末休息','系统按周末自动生成'),(53,'2026-07-04','REST','周末休息','系统按周末自动生成'),(54,'2026-07-05','REST','周末休息','系统按周末自动生成'),(55,'2026-07-11','REST','周末休息','系统按周末自动生成'),(56,'2026-07-12','REST','周末休息','系统按周末自动生成'),(57,'2026-07-18','REST','周末休息','系统按周末自动生成'),(58,'2026-07-19','REST','周末休息','系统按周末自动生成'),(59,'2026-07-25','REST','周末休息','系统按周末自动生成'),(60,'2026-07-26','REST','周末休息','系统按周末自动生成'),(61,'2026-08-01','REST','周末休息','系统按周末自动生成'),(62,'2026-08-02','REST','周末休息','系统按周末自动生成'),(63,'2026-08-08','REST','周末休息','系统按周末自动生成'),(64,'2026-08-09','REST','周末休息','系统按周末自动生成'),(65,'2026-08-15','REST','周末休息','系统按周末自动生成'),(66,'2026-08-16','REST','周末休息','系统按周末自动生成'),(67,'2026-08-22','REST','周末休息','系统按周末自动生成'),(68,'2026-08-23','REST','周末休息','系统按周末自动生成'),(69,'2026-08-29','REST','周末休息','系统按周末自动生成'),(70,'2026-08-30','REST','周末休息','系统按周末自动生成'),(71,'2026-09-05','REST','周末休息','系统按周末自动生成'),(72,'2026-09-06','REST','周末休息','系统按周末自动生成'),(73,'2026-09-12','REST','周末休息','系统按周末自动生成'),(74,'2026-09-13','REST','周末休息','系统按周末自动生成'),(75,'2026-09-19','REST','周末休息','系统按周末自动生成'),(76,'2026-09-20','REST','周末休息','系统按周末自动生成'),(77,'2026-09-26','REST','周末休息','系统按周末自动生成'),(78,'2026-09-27','REST','周末休息','系统按周末自动生成'),(79,'2026-10-03','REST','周末休息','系统按周末自动生成'),(80,'2026-10-04','REST','周末休息','系统按周末自动生成'),(81,'2026-10-10','REST','周末休息','系统按周末自动生成'),(82,'2026-10-11','REST','周末休息','系统按周末自动生成'),(83,'2026-10-17','REST','周末休息','系统按周末自动生成'),(84,'2026-10-18','REST','周末休息','系统按周末自动生成'),(85,'2026-10-24','REST','周末休息','系统按周末自动生成'),(86,'2026-10-25','REST','周末休息','系统按周末自动生成'),(87,'2026-10-31','REST','周末休息','系统按周末自动生成'),(88,'2026-11-01','REST','周末休息','系统按周末自动生成'),(89,'2026-11-07','REST','周末休息','系统按周末自动生成'),(90,'2026-11-08','REST','周末休息','系统按周末自动生成'),(91,'2026-11-14','REST','周末休息','系统按周末自动生成'),(92,'2026-11-15','REST','周末休息','系统按周末自动生成'),(93,'2026-11-21','REST','周末休息','系统按周末自动生成'),(94,'2026-11-22','REST','周末休息','系统按周末自动生成'),(95,'2026-11-28','REST','周末休息','系统按周末自动生成'),(96,'2026-11-29','REST','周末休息','系统按周末自动生成'),(97,'2026-12-05','REST','周末休息','系统按周末自动生成'),(98,'2026-12-06','REST','周末休息','系统按周末自动生成'),(99,'2026-12-12','REST','周末休息','系统按周末自动生成'),(100,'2026-12-13','REST','周末休息','系统按周末自动生成'),(101,'2026-12-19','REST','周末休息','系统按周末自动生成'),(102,'2026-12-20','REST','周末休息','系统按周末自动生成'),(103,'2026-12-26','REST','周末休息','系统按周末自动生成'),(104,'2026-12-27','REST','周末休息','系统按周末自动生成'),(128,'2026-01-01','HOLIDAY','元旦',NULL),(129,'2026-05-01','HOLIDAY','劳动节',NULL),(130,'2026-10-01','HOLIDAY','国庆节',NULL),(131,'2026-10-02','HOLIDAY','国庆节',NULL);
/*!40000 ALTER TABLE `holiday_calendar` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `leave_balance`
--

DROP TABLE IF EXISTS `leave_balance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leave_balance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL,
  `year_num` int NOT NULL COMMENT '额度年度',
  `type_code` varchar(30) NOT NULL COMMENT '假别编码',
  `total_days` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '总额度',
  `used_days` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '已使用',
  `remain_days` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '剩余',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_balance` (`employee_id`,`year_num`,`type_code`)
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='假期余额表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `leave_balance`
--

LOCK TABLES `leave_balance` WRITE;
/*!40000 ALTER TABLE `leave_balance` DISABLE KEYS */;
/*!40000 ALTER TABLE `leave_balance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `leave_request`
--

DROP TABLE IF EXISTS `leave_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leave_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `type` varchar(20) NOT NULL COMMENT '请假类型: SICK/PERSONAL/ANNUAL',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `reason` varchar(500) DEFAULT NULL COMMENT '请假事由',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '审批状态: PENDING/APPROVED/REJECTED',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `approver_id` bigint DEFAULT NULL COMMENT '瀹℃壒浜篒D',
  `approve_time` datetime DEFAULT NULL COMMENT '瀹℃壒鏃堕棿',
  `approve_comment` varchar(200) DEFAULT NULL COMMENT '瀹℃壒鎰忚?',
  `days` decimal(4,1) NOT NULL DEFAULT '1.0' COMMENT '请假天数',
  `type_code` varchar(30) DEFAULT NULL COMMENT '假别编码',
  `approve_remark` varchar(300) DEFAULT NULL,
  `current_step` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_leave_status` (`status`),
  KEY `idx_leave_emp` (`employee_id`),
  CONSTRAINT `fk_leave_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `leave_request`
--

LOCK TABLES `leave_request` WRITE;
/*!40000 ALTER TABLE `leave_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `leave_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `leave_type`
--

DROP TABLE IF EXISTS `leave_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leave_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(30) NOT NULL COMMENT '假别编码',
  `name` varchar(50) NOT NULL COMMENT '假别名称',
  `annual_quota` decimal(5,1) NOT NULL DEFAULT '0.0' COMMENT '年度额度(天),0表示不限额',
  `paid` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否带薪',
  `need_approval` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否需要审批',
  `carry_forward` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否可结转次年',
  `deduct_rule` varchar(20) NOT NULL DEFAULT 'WORKDAY' COMMENT 'WORKDAY仅扣工作日/CALENDAR按自然日',
  `color` varchar(20) NOT NULL DEFAULT '#4f6bed',
  `sort_no` int NOT NULL DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='假期类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `leave_type`
--

LOCK TABLES `leave_type` WRITE;
/*!40000 ALTER TABLE `leave_type` DISABLE KEYS */;
INSERT INTO `leave_type` VALUES (1,'PERSONAL','事假',0.0,0,1,0,'WORKDAY','#868e96',1,1),(2,'SICK','病假',0.0,1,1,0,'WORKDAY','#fa5252',2,1),(3,'ANNUAL','年假',10.0,1,1,1,'WORKDAY','#4f6bed',3,1),(4,'COMP','调休',0.0,1,1,0,'WORKDAY','#12b886',4,1),(5,'MARRIAGE','婚假',10.0,1,1,0,'CALENDAR','#f06595',5,1),(6,'MATERNITY','产假',98.0,1,1,0,'CALENDAR','#7950f2',6,1),(7,'BEREAVE','丧假',3.0,1,1,0,'CALENDAR','#adb5bd',7,1);
/*!40000 ALTER TABLE `leave_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `login_log`
--

DROP TABLE IF EXISTS `login_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `ip` varchar(50) DEFAULT NULL,
  `login_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `success` tinyint(1) DEFAULT '1',
  `user_agent` varchar(200) DEFAULT NULL COMMENT '客户端UA',
  PRIMARY KEY (`id`),
  KEY `idx_log_time` (`login_time`),
  KEY `idx_log_user_time` (`username`,`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=132 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `login_log`
--

LOCK TABLES `login_log` WRITE;
/*!40000 ALTER TABLE `login_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `login_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `makeup_request`
--

DROP TABLE IF EXISTS `makeup_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `makeup_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL,
  `miss_date` date NOT NULL COMMENT '漏打卡日期',
  `punch_type` varchar(10) NOT NULL COMMENT 'IN上班卡/OUT下班卡',
  `makeup_time` datetime NOT NULL COMMENT '补卡时间',
  `reason` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `approver_id` bigint DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `approve_remark` varchar(300) DEFAULT NULL,
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='补卡申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `makeup_request`
--

LOCK TABLES `makeup_request` WRITE;
/*!40000 ALTER TABLE `makeup_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `makeup_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `overtime_request`
--

DROP TABLE IF EXISTS `overtime_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `overtime_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL,
  `ot_date` date NOT NULL COMMENT '加班日期',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `hours` decimal(5,1) NOT NULL COMMENT '加班时长(小时)',
  `ot_type` varchar(20) NOT NULL DEFAULT 'WORKDAY' COMMENT 'WORKDAY工作日/WEEKEND周末/HOLIDAY节假日',
  `compensate_type` varchar(20) NOT NULL DEFAULT 'COMP' COMMENT 'COMP转调休/FEE加班费',
  `reason` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
  `current_step` int NOT NULL DEFAULT '1',
  `approver_id` bigint DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `approve_remark` varchar(300) DEFAULT NULL,
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='加班申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `overtime_request`
--

LOCK TABLES `overtime_request` WRITE;
/*!40000 ALTER TABLE `overtime_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `overtime_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `punch_location`
--

DROP TABLE IF EXISTS `punch_location`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `punch_location` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(80) NOT NULL COMMENT '地点名称',
  `address` varchar(200) DEFAULT NULL COMMENT '详细地址',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '中心经度',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '中心纬度',
  `radius_m` int NOT NULL DEFAULT '200' COMMENT '允许半径(米)',
  `allow_ip` varchar(300) DEFAULT NULL COMMENT '允许IP,逗号分隔,支持 192.168.1.* 与 /24 CIDR',
  `wifi_ssid` varchar(100) DEFAULT NULL COMMENT '允许WiFi SSID',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否作为未分组员工的默认地点',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `remark` varchar(300) DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL COMMENT '设置人账号',
  `created_by_role` varchar(30) DEFAULT NULL COMMENT '设置人层级 SUPER_ADMIN/ADMIN',
  `updated_by` varchar(50) DEFAULT NULL COMMENT '最后修改人',
  `updated_by_role` varchar(30) DEFAULT NULL COMMENT '最后修改人层级',
  `update_time` datetime DEFAULT NULL COMMENT '最后修改时间（北京时间）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=922 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打卡地点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `punch_location`
--

LOCK TABLES `punch_location` WRITE;
/*!40000 ALTER TABLE `punch_location` DISABLE KEYS */;
/*!40000 ALTER TABLE `punch_location` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule`
--

DROP TABLE IF EXISTS `schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `work_date` date NOT NULL COMMENT '排班日期',
  `shift_id` bigint DEFAULT NULL COMMENT '班次ID,NULL且is_rest=1为休息',
  `is_rest` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否休息日',
  `source` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL手工/AUTO周期生成/IMPORT导入',
  `remark` varchar(200) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schedule_emp_date` (`employee_id`,`work_date`),
  KEY `idx_schedule_date` (`work_date`)
) ENGINE=InnoDB AUTO_INCREMENT=3134 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工排班表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule`
--

LOCK TABLES `schedule` WRITE;
/*!40000 ALTER TABLE `schedule` DISABLE KEYS */;
/*!40000 ALTER TABLE `schedule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shift`
--

DROP TABLE IF EXISTS `shift`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shift` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(30) NOT NULL COMMENT '班次编码',
  `name` varchar(50) NOT NULL COMMENT '班次名称',
  `start_time` time NOT NULL COMMENT '上班时间',
  `end_time` time NOT NULL COMMENT '下班时间',
  `late_grace` int NOT NULL DEFAULT '0' COMMENT '迟到宽限(分钟)',
  `early_grace` int NOT NULL DEFAULT '0' COMMENT '早退宽限(分钟)',
  `flex_minutes` int NOT NULL DEFAULT '0' COMMENT '弹性打卡(分钟),0为固定班',
  `work_hours` decimal(4,1) NOT NULL DEFAULT '8.0' COMMENT '标准工时(小时)',
  `cross_day` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否跨天班',
  `color` varchar(20) NOT NULL DEFAULT '#4f6bed' COMMENT '日历显示色',
  `remark` varchar(200) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班次表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shift`
--

LOCK TABLES `shift` WRITE;
/*!40000 ALTER TABLE `shift` DISABLE KEYS */;
INSERT INTO `shift` VALUES (1,'S-ADMIN','行政班(弹性30分钟)','09:00:00','18:00:00',10,10,30,8.0,0,'#4f6bed','默认班次，含1小时午休',1,'2026-11-21 15:27:58'),(2,'S-MORNING','早班','08:00:00','16:00:00',5,5,0,8.0,0,'#12b886','生产与客服早班',1,'2026-11-21 15:27:58'),(3,'S-EVENING','晚班','14:00:00','22:00:00',5,5,0,8.0,0,'#f59f00','生产与客服晚班',1,'2026-11-21 15:27:58'),(4,'S-NIGHT','夜班(跨天)','22:00:00','06:00:00',5,5,0,8.0,1,'#845ef7','跨天班次',1,'2026-11-21 15:27:58'),(5,'S-FLEX','自由工时','09:00:00','18:00:00',0,0,480,8.0,0,'#e8590c','不判定迟到早退，仅统计工时',1,'2026-11-21 15:27:58');
/*!40000 ALTER TABLE `shift` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_account_log`
--

DROP TABLE IF EXISTS `sys_account_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_account_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator` varchar(50) NOT NULL COMMENT '操作人账号',
  `operator_role` varchar(30) NOT NULL COMMENT '操作人角色',
  `action` varchar(30) NOT NULL COMMENT 'CREATE/UPDATE/DELETE/ENABLE/DISABLE/RESET_PWD/GRANT',
  `target_user` varchar(50) NOT NULL COMMENT '目标账号',
  `target_role` varchar(30) DEFAULT NULL COMMENT '目标角色',
  `detail` varchar(500) DEFAULT NULL COMMENT '变更摘要',
  `ip` varchar(64) DEFAULT NULL,
  `result` varchar(20) NOT NULL DEFAULT 'SUCCESS',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_al_time` (`create_time`),
  KEY `idx_al_target` (`target_user`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账户管理操作记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_account_log`
--

LOCK TABLES `sys_account_log` WRITE;
/*!40000 ALTER TABLE `sys_account_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_account_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父菜单ID,0为一级',
  `name` varchar(50) NOT NULL COMMENT '菜单名称',
  `icon` varchar(50) DEFAULT NULL COMMENT '图标标识',
  `url` varchar(120) DEFAULT NULL COMMENT '路由地址',
  `perm_code` varchar(80) DEFAULT NULL COMMENT '权限标识',
  `menu_type` varchar(20) NOT NULL DEFAULT 'MENU' COMMENT 'DIR目录/MENU菜单',
  `sort_no` int NOT NULL DEFAULT '0',
  `visible` tinyint(1) NOT NULL DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=89 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单与权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES (1,0,'工作台','grid','','dashboard','DIR',1,1,'2026-11-21 15:27:58'),(2,0,'组织与人员','users','','org','DIR',2,1,'2026-11-21 15:27:58'),(3,0,'考勤管理','calendar','','att','DIR',3,1,'2026-11-21 15:27:58'),(4,0,'审批中心','check','','approval','DIR',4,1,'2026-11-21 15:27:58'),(5,0,'统计报表','chart','','report','DIR',5,1,'2026-11-21 15:27:58'),(6,0,'系统管理','settings','','system','DIR',6,1,'2026-11-21 15:27:58'),(7,0,'员工自助','user','','self','DIR',7,1,'2026-11-21 15:27:58'),(8,0,'超级管理员','shield','','super','DIR',0,1,'2026-11-21 16:11:39'),(11,1,'管理员工作台','grid','/dashboard','dashboard:admin','MENU',1,1,'2026-11-21 15:27:58'),(12,1,'我的工作台','home','/my/home','dashboard:self','MENU',2,1,'2026-11-21 15:27:58'),(21,2,'部门架构','tree','/admin/org/departments','org:dept','MENU',1,1,'2026-11-21 15:27:58'),(22,2,'员工档案','users','/admin/employees','org:employee','MENU',2,1,'2026-11-21 15:27:58'),(23,2,'考勤组','layers','/admin/groups','org:group','MENU',3,1,'2026-11-21 15:27:58'),(31,3,'班次管理','clock','/admin/shifts','att:shift','MENU',1,1,'2026-11-21 15:27:58'),(32,3,'排班日历','calendar','/admin/schedule','att:schedule','MENU',2,1,'2026-11-21 15:27:58'),(33,3,'工作日历','calendar','/admin/holidays','att:holiday','MENU',3,1,'2026-11-21 15:27:58'),(34,3,'打卡流水','list','/admin/attendance/records','att:record','MENU',4,1,'2026-11-21 15:27:58'),(35,3,'异常考勤','alert','/admin/attendance/exceptions','att:exception','MENU',5,1,'2026-11-21 15:27:58'),(36,3,'月度汇总','table','/admin/attendance/summary','att:summary','MENU',6,1,'2026-11-21 15:27:58'),(37,3,'假别与额度','sliders','/admin/leave-types','att:leavetype','MENU',7,1,'2026-11-21 15:27:58'),(38,3,'假期余额','pie','/admin/leave-balances','att:balance','MENU',8,1,'2026-11-21 15:27:58'),(39,3,'打卡地点','pin','/admin/locations','att:location','MENU',9,1,'2026-11-21 15:51:59'),(41,4,'待办审批','inbox','/admin/approval/pending','approval:pending','MENU',1,1,'2026-11-21 15:27:58'),(42,4,'已办审批','history','/admin/approval/done','approval:done','MENU',2,1,'2026-11-21 15:27:58'),(43,4,'加班记录','plus','/admin/overtime','approval:overtime','MENU',3,1,'2026-11-21 15:27:58'),(44,4,'出差记录','plane','/admin/trips','approval:trip','MENU',4,1,'2026-11-21 15:27:58'),(45,4,'补卡记录','edit','/admin/makeups','approval:makeup','MENU',5,1,'2026-11-21 15:27:58'),(51,5,'考勤报表','chart','/admin/report','report:view','MENU',1,1,'2026-11-21 15:27:58'),(52,5,'数据导出','download','/admin/report/export','report:export','MENU',2,1,'2026-11-21 15:27:58'),(61,6,'员工账号','key','/admin/system/users','system:user','MENU',1,1,'2026-11-21 15:27:58'),(62,6,'角色权限','shield','/admin/system/roles','system:role','MENU',2,1,'2026-11-21 15:27:58'),(63,6,'公告通知','bell','/admin/system/notices','system:notice','MENU',3,1,'2026-11-21 15:27:58'),(64,6,'操作日志','file','/admin/system/oper-logs','system:log','MENU',4,1,'2026-11-21 15:27:58'),(65,6,'登录日志','activity','/admin/login-logs','system:loginlog','MENU',5,1,'2026-11-21 15:27:58'),(71,7,'我的考勤','list','/my/attendance','self:attendance','MENU',1,1,'2026-11-21 15:27:58'),(72,7,'我的班表','calendar','/my/schedule','self:schedule','MENU',2,1,'2026-11-21 15:27:58'),(73,7,'我的请假','file-text','/my/leave','self:leave','MENU',3,1,'2026-11-21 15:27:58'),(74,7,'我的加班','plus','/my/overtime','self:overtime','MENU',4,1,'2026-11-21 15:27:58'),(75,7,'我的出差','plane','/my/trip','self:trip','MENU',5,1,'2026-11-21 15:27:58'),(76,7,'我的补卡','edit','/my/makeup','self:makeup','MENU',6,1,'2026-11-21 15:27:58'),(77,7,'假期余额','pie','/my/balance','self:balance','MENU',7,1,'2026-11-21 15:27:58'),(78,7,'个人中心','user','/my/profile','self:profile','MENU',8,1,'2026-11-21 15:27:58'),(81,8,'超级工作台','grid','/super/dashboard','super:dashboard','MENU',1,1,'2026-11-21 16:11:39'),(82,8,'管理员账户','users','/super/admins','super:admin','MENU',2,1,'2026-11-21 16:11:39'),(83,8,'角色权限','key','/super/roles','super:role','MENU',3,1,'2026-11-21 16:11:39'),(84,8,'账户操作记录','file','/super/account-logs','super:accountlog','MENU',4,1,'2026-11-21 16:11:39'),(85,8,'系统配置','settings','/super/config','super:config','MENU',5,1,'2026-11-21 16:11:39'),(86,8,'员工账号','users','/super/employees','super:employee','MENU',6,1,'2026-11-21 17:59:51'),(87,8,'打卡地点(覆盖)','map-pin','/super/locations','super:location','MENU',7,1,'2026-11-21 17:59:51'),(88,8,'权限边界','list','/super/permissions','super:permission','MENU',8,1,'2026-11-21 17:59:51');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notice`
--

DROP TABLE IF EXISTS `sys_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(150) NOT NULL,
  `content` text,
  `notice_type` varchar(20) NOT NULL DEFAULT 'NOTICE' COMMENT 'NOTICE通知/POLICY制度/URGENT紧急',
  `publisher` varchar(50) DEFAULT NULL,
  `publish_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `pinned` tinyint(1) NOT NULL DEFAULT '0',
  `view_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告通知表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notice`
--

LOCK TABLES `sys_notice` WRITE;
/*!40000 ALTER TABLE `sys_notice` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `module` varchar(50) DEFAULT NULL COMMENT '业务模块',
  `operation` varchar(100) DEFAULT NULL COMMENT '操作名称',
  `request_method` varchar(10) DEFAULT NULL,
  `request_url` varchar(200) DEFAULT NULL,
  `ip` varchar(50) DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL,
  `params` text,
  `result` varchar(20) DEFAULT NULL COMMENT 'SUCCESS/ERROR',
  `cost_ms` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_oper_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=272 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(30) NOT NULL COMMENT '角色编码',
  `name` varchar(50) NOT NULL COMMENT '角色名称',
  `description` varchar(200) DEFAULT NULL COMMENT '角色说明',
  `data_scope` varchar(20) NOT NULL DEFAULT 'SELF' COMMENT '数据范围: ALL全公司/DEPT本部门/SELF仅本人',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'ADMIN','管理员','管理员子系统：管理员工账户、组织人事、考勤规则、审批与报表','ALL',1,'2026-11-21 15:27:58'),(2,'HR','人事考勤员','管理员子系统（人事岗）：员工账户与考勤核算','ALL',1,'2026-11-21 15:27:58'),(3,'MANAGER','部门负责人','管理员子系统（部门岗）：本部门考勤与审批','DEPT',1,'2026-11-21 15:27:58'),(4,'EMPLOYEE','员工','员工子系统：仅本人信息与自助功能入口','SELF',1,'2026-11-21 15:27:58'),(5,'SUPER_ADMIN','超级管理员','主管理系统最高权限：统一管理管理员账户、角色权限、系统配置与审计','ALL',1,'2026-11-21 16:10:09');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,11),(1,12),(1,21),(1,22),(1,23),(1,31),(1,32),(1,33),(1,34),(1,35),(1,36),(1,37),(1,38),(1,39),(1,41),(1,42),(1,43),(1,44),(1,45),(1,51),(1,52),(1,61),(1,62),(1,63),(1,64),(1,65),(1,71),(1,72),(1,73),(1,74),(1,75),(1,76),(1,77),(1,78),(2,1),(2,2),(2,3),(2,4),(2,5),(2,6),(2,7),(2,11),(2,12),(2,21),(2,22),(2,23),(2,31),(2,32),(2,33),(2,34),(2,35),(2,36),(2,37),(2,38),(2,39),(2,41),(2,42),(2,43),(2,44),(2,45),(2,51),(2,52),(2,61),(2,63),(2,64),(2,65),(2,71),(2,72),(2,73),(2,74),(2,75),(2,76),(2,77),(2,78),(3,1),(3,2),(3,3),(3,4),(3,5),(3,7),(3,11),(3,21),(3,22),(3,32),(3,34),(3,35),(3,36),(3,41),(3,42),(3,43),(3,44),(3,45),(3,51),(3,52),(3,71),(3,72),(3,73),(3,74),(3,75),(3,76),(3,77),(3,78),(4,7),(4,12),(4,71),(4,72),(4,73),(4,74),(4,75),(4,76),(4,77),(4,78),(5,1),(5,2),(5,3),(5,4),(5,5),(5,6),(5,7),(5,8),(5,11),(5,12),(5,21),(5,22),(5,23),(5,31),(5,32),(5,33),(5,34),(5,35),(5,36),(5,37),(5,38),(5,39),(5,41),(5,42),(5,43),(5,44),(5,45),(5,51),(5,52),(5,61),(5,62),(5,63),(5,64),(5,65),(5,71),(5,72),(5,73),(5,74),(5,75),(5,76),(5,77),(5,78),(5,81),(5,82),(5,83),(5,84),(5,85),(5,86),(5,87),(5,88);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '登录用户名',
  `password` varchar(255) NOT NULL COMMENT '密码(BCrypt加密)',
  `plain_password` varchar(255) DEFAULT NULL COMMENT '明文密码(管理员查看用)',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `role` varchar(20) NOT NULL DEFAULT 'EMPLOYEE' COMMENT '角色: ADMIN/EMPLOYEE',
  `employee_id` bigint DEFAULT NULL COMMENT '关联员工ID',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(200) DEFAULT NULL COMMENT '头像',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人账号',
  `role_level` varchar(20) DEFAULT NULL COMMENT '账户层级 SUPER/ADMIN/EMPLOYEE',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_user_emp` (`employee_id`)
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'attendance_db'
--

--
-- Dumping routines for database 'attendance_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-22 22:14:15
