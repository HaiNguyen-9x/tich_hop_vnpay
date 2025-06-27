-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: tich_hop
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `refunds`
--

DROP TABLE IF EXISTS `refunds`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refunds` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` bigint DEFAULT NULL,
  `status` int NOT NULL,
  `timestamp` varchar(255) DEFAULT NULL,
  `transaction_info` varchar(255) DEFAULT NULL,
  `payment_id` bigint NOT NULL,
  `response_code` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpt9ic0j1y6xwlej99wnynvnpy` (`payment_id`),
  CONSTRAINT `FKpt9ic0j1y6xwlej99wnynvnpy` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `refunds`
--

LOCK TABLES `refunds` WRITE;
/*!40000 ALTER TABLE `refunds` DISABLE KEYS */;
INSERT INTO `refunds` VALUES (1,6000000,1,'2025-06-25 11:08:10','Hoan tra don hang 449646400',106,'00'),(2,30000000,2,'2025-06-25 11:13:46','Hoan tra don hang 449646400',106,'94'),(3,30000000,2,'2025-06-25 11:20:06','Hoan tra don hang 449646400',106,'94'),(4,190000,1,'2025-06-25 11:21:50','Hoan tra don hang 399939538',107,'00'),(5,100000,2,'2025-06-25 11:25:16','Hoan tra don hang 399939538',107,'94'),(6,20000000,2,'2025-06-26 08:08:42','Hoan tra don hang 449646400',106,'94'),(7,36000,1,'2025-06-26 08:37:05','Hoan tra don hang 1645988068',108,'00'),(8,20000,1,'2025-06-26 08:38:26','Hoan tra don hang 1893004042',109,'00'),(9,50000,2,'2025-06-26 08:38:40','Hoan tra don hang 1893004042',109,'94'),(10,5000,2,'2025-06-26 10:01:34','Hoan tra don hang 642177140',70,'99'),(11,10,2,'2025-06-26 10:11:22','Hoan tra don hang 2108930425',99,'94'),(12,10,2,'2025-06-26 10:11:22','Hoan tra don hang 2108930425',99,'94'),(13,10,2,'2025-06-26 10:11:37','Hoan tra don hang 1329104349',13,'97'),(14,10,2,'2025-06-26 10:12:09','Hoan tra don hang 1528758201',15,'97'),(15,10000,2,'2025-06-27 08:52:17','Hoan tra don hang 449646400',106,'94'),(16,100000000,1,'2025-06-27 09:15:40','Hoan tra don hang 620587668',110,'00'),(17,100000,2,'2025-06-27 10:23:16','Hoan tra don hang 1893004042',109,'94'),(18,500000,2,'2025-06-27 14:04:45','Hoan tra don hang 726204260',4,'97'),(19,20000,1,'2025-06-27 14:07:40','Hoan tra don hang 1805337659',2,'00'),(20,250000,2,'2025-06-27 16:15:39','Hoan tra don hang 148830194',111,'97'),(21,250000,2,'2025-06-27 16:16:13','Hoan tra don hang 148830194',111,'94'),(22,270000,1,'2025-06-27 16:17:37','Hoan tra don hang 2105321071',112,'00'),(23,10000,1,'2025-06-27 16:18:40','Hoan tra don hang 1997553715',113,'00'),(24,13000,1,'2025-06-27 14:32:19','Hoan tra don hang 477530085',114,'00'),(25,130000,1,'2025-06-27 16:36:35','Hoan tra don hang 1354325498',115,'00');
/*!40000 ALTER TABLE `refunds` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-06-27 17:19:00
