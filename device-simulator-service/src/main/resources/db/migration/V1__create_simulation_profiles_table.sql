CREATE TABLE `simulation_profiles` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `profile_name` varchar(255) NOT NULL,
  `is_enabled` tinyint(1) NOT NULL,
  `profile_config` text NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_profile_name` (`profile_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;