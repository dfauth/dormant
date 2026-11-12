--liquibase formatted sql
--changeset market-holiday-calendar:1

CREATE TABLE IF NOT EXISTS market_holiday_calendar (
                                         exchange_mic VARCHAR(4) NOT NULL,
                                         holiday_date DATE NOT NULL,
                                         holiday_name VARCHAR(100) NOT NULL,
                                         market_status VARCHAR(20) NOT NULL CHECK (market_status IN ('CLOSED', 'EARLY_CLOSE')),
                                         early_close_time_local TIME NULL,
                                         PRIMARY KEY (exchange_mic, holiday_date)
);

-- Index to optimize quick multi-exchange status checks for a single date
CREATE INDEX IF NOT EXISTS idx_holiday_lookup ON market_holiday_calendar (holiday_date);

INSERT INTO MARKET_HOLIDAY_CALENDAR (exchange_mic, holiday_date, holiday_name, market_status, early_close_time_local) VALUES
-- ==========================================
-- AUSTRALIAN SECURITIES EXCHANGE (XASX)
-- ==========================================
-- 2016
('XASX', '2016-01-01', 'New Year''s Day', 'CLOSED', NULL),
('XASX', '2016-01-26', 'Australia Day', 'CLOSED', NULL),
('XASX', '2016-03-24', 'Day before Good Friday', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2016-03-25', 'Good Friday', 'CLOSED', NULL),
('XASX', '2016-03-28', 'Easter Monday', 'CLOSED', NULL),
('XASX', '2016-04-25', 'Anzac Day', 'CLOSED', NULL),
('XASX', '2016-06-13', 'King''s Birthday', 'CLOSED', NULL),
('XASX', '2016-12-23', 'Day before Christmas (Early Close Shift)', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2016-12-26', 'Christmas Day (Substitute)', 'CLOSED', NULL),
('XASX', '2016-12-27', 'Boxing Day (Substitute)', 'CLOSED', NULL),
('XASX', '2016-12-30', 'Day before New Year (Early Close Shift)', 'EARLY_CLOSE', '14:10:00'),

-- 2017
('XASX', '2017-01-02', 'New Year''s Day (Substitute)', 'CLOSED', NULL),
('XASX', '2017-01-26', 'Australia Day', 'CLOSED', NULL),
('XASX', '2017-04-13', 'Day before Good Friday', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2017-04-14', 'Good Friday', 'CLOSED', NULL),
('XASX', '2017-04-17', 'Easter Monday', 'CLOSED', NULL),
('XASX', '2017-04-25', 'Anzac Day', 'CLOSED', NULL),
('XASX', '2017-06-12', 'King''s Birthday', 'CLOSED', NULL),
('XASX', '2017-12-22', 'Day before Christmas (Early Close Shift)', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2017-12-25', 'Christmas Day', 'CLOSED', NULL),
('XASX', '2017-12-26', 'Boxing Day', 'CLOSED', NULL),
('XASX', '2017-12-29', 'Day before New Year (Early Close Shift)', 'EARLY_CLOSE', '14:10:00'),

-- 2018
('XASX', '2018-01-01', 'New Year''s Day', 'CLOSED', NULL),
('XASX', '2018-01-26', 'Australia Day', 'CLOSED', NULL),
('XASX', '2018-03-29', 'Day before Good Friday', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2018-03-30', 'Good Friday', 'CLOSED', NULL),
('XASX', '2018-04-02', 'Easter Monday', 'CLOSED', NULL),
('XASX', '2018-04-25', 'Anzac Day', 'CLOSED', NULL),
('XASX', '2018-06-11', 'King''s Birthday', 'CLOSED', NULL),
('XASX', '2018-12-24', 'Christmas Eve', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2018-12-25', 'Christmas Day', 'CLOSED', NULL),
('XASX', '2018-12-26', 'Boxing Day', 'CLOSED', NULL),
('XASX', '2018-12-31', 'New Years Eve', 'EARLY_CLOSE', '14:10:00'),

-- 2019
('XASX', '2019-01-01', 'New Year''s Day', 'CLOSED', NULL),
('XASX', '2019-01-28', 'Australia Day (Substitute)', 'CLOSED', NULL),
('XASX', '2019-04-18', 'Day before Good Friday', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2019-04-19', 'Good Friday', 'CLOSED', NULL),
('XASX', '2019-04-22', 'Easter Monday', 'CLOSED', NULL),
('XASX', '2019-04-25', 'Anzac Day', 'CLOSED', NULL),
('XASX', '2019-06-10', 'King''s Birthday', 'CLOSED', NULL),
('XASX', '2019-12-24', 'Christmas Eve', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2019-12-25', 'Christmas Day', 'CLOSED', NULL),
('XASX', '2019-12-26', 'Boxing Day', 'CLOSED', NULL),
('XASX', '2019-12-31', 'New Years Eve', 'EARLY_CLOSE', '14:10:00'),

-- 2020
('XASX', '2020-01-01', 'New Year''s Day', 'CLOSED', NULL),
('XASX', '2020-01-27', 'Australia Day (Substitute)', 'CLOSED', NULL),
('XASX', '2020-04-09', 'Day before Good Friday', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2020-04-10', 'Good Friday', 'CLOSED', NULL),
('XASX', '2020-04-13', 'Easter Monday', 'CLOSED', NULL),
('XASX', '2020-04-25', 'Anzac Day', 'CLOSED', NULL),
('XASX', '2020-06-08', 'King''s Birthday', 'CLOSED', NULL),
('XASX', '2020-12-24', 'Christmas Eve', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2020-12-25', 'Christmas Day', 'CLOSED', NULL),
('XASX', '2020-12-28', 'Boxing Day (Substitute)', 'CLOSED', NULL),
('XASX', '2020-12-31', 'New Years Eve', 'EARLY_CLOSE', '14:10:00'),

-- 2021
('XASX', '2021-01-01', 'New Year''s Day', 'CLOSED', NULL),
('XASX', '2021-01-26', 'Australia Day', 'CLOSED', NULL),
('XASX', '2021-04-01', 'Day before Good Friday', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2021-04-02', 'Good Friday', 'CLOSED', NULL),
('XASX', '2021-04-05', 'Easter Monday', 'CLOSED', NULL),
('XASX', '2021-04-26', 'Anzac Day (Substitute)', 'CLOSED', NULL),
('XASX', '2021-06-14', 'King''s Birthday', 'CLOSED', NULL),
('XASX', '2021-12-24', 'Christmas Eve', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2021-12-27', 'Christmas Day (Substitute)', 'CLOSED', NULL),
('XASX', '2021-12-28', 'Boxing Day (Substitute)', 'CLOSED', NULL),
('XASX', '2021-12-31', 'New Years Eve', 'EARLY_CLOSE', '14:10:00'),

-- 2022
('XASX', '2022-01-03', 'New Year''s Day (Substitute)', 'CLOSED', NULL),
('XASX', '2022-01-26', 'Australia Day', 'CLOSED', NULL),
('XASX', '2022-04-14', 'Day before Good Friday', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2022-04-15', 'Good Friday', 'CLOSED', NULL),
('XASX', '2022-04-18', 'Easter Monday', 'CLOSED', NULL),
('XASX', '2022-04-25', 'Anzac Day', 'CLOSED', NULL),
('XASX', '2022-06-13', 'King''s Birthday', 'CLOSED', NULL),
('XASX', '2022-09-22', 'National Day of Mourning for Queen Elizabeth II (Special Exception)', 'CLOSED', NULL),
('XASX', '2022-12-23', 'Day before Christmas (Early Close Shift)', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2022-12-26', 'Boxing Day', 'CLOSED', NULL),
('XASX', '2022-12-27', 'Christmas Day (Substitute)', 'CLOSED', NULL),
('XASX', '2022-12-30', 'Day before New Year (Early Close Shift)', 'EARLY_CLOSE', '14:10:00'),

-- 2023
('XASX', '2023-01-02', 'New Year''s Day (Substitute)', 'CLOSED', NULL),
('XASX', '2023-01-26', 'Australia Day', 'CLOSED', NULL),
('XASX', '2023-04-06', 'Day before Good Friday', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2023-04-07', 'Good Friday', 'CLOSED', NULL),
('XASX', '2023-04-10', 'Easter Monday', 'CLOSED', NULL),
('XASX', '2023-04-25', 'Anzac Day', 'CLOSED', NULL),
('XASX', '2023-06-12', 'King''s Birthday', 'CLOSED', NULL),
('XASX', '2023-12-22', 'Day before Christmas (Early Close Shift)', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2023-12-25', 'Christmas Day', 'CLOSED', NULL),
('XASX', '2023-12-26', 'Boxing Day', 'CLOSED', NULL),
('XASX', '2023-12-29', 'Day before New Year (Early Close Shift)', 'EARLY_CLOSE', '14:10:00'),

-- 2024
('XASX', '2024-01-01', 'New Year''s Day', 'CLOSED', NULL),
('XASX', '2024-01-26', 'Australia Day', 'CLOSED', NULL),
('XASX', '2024-03-28', 'Day before Good Friday', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2024-03-29', 'Good Friday', 'CLOSED', NULL),
('XASX', '2024-04-01', 'Easter Monday', 'CLOSED', NULL),
('XASX', '2024-04-25', 'Anzac Day', 'CLOSED', NULL),
('XASX', '2024-06-10', 'King''s Birthday', 'CLOSED', NULL),
('XASX', '2024-12-24', 'Christmas Eve', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2024-12-25', 'Christmas Day', 'CLOSED', NULL),
('XASX', '2024-12-26', 'Boxing Day', 'CLOSED', NULL),
('XASX', '2024-12-31', 'New Years Eve', 'EARLY_CLOSE', '14:10:00'),

-- 2025
('XASX', '2025-01-01', 'New Year''s Day', 'CLOSED', NULL),
('XASX', '2025-01-27', 'Australia Day (Substitute)', 'CLOSED', NULL),
('XASX', '2025-04-17', 'Day before Good Friday', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2025-04-18', 'Good Friday', 'CLOSED', NULL),
('XASX', '2025-04-21', 'Easter Monday', 'CLOSED', NULL),
('XASX', '2025-04-25', 'Anzac Day', 'CLOSED', NULL),
('XASX', '2025-06-09', 'King''s Birthday', 'CLOSED', NULL),
('XASX', '2025-12-24', 'Christmas Eve', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2025-12-25', 'Christmas Day', 'CLOSED', NULL),
('XASX', '2025-12-26', 'Boxing Day', 'CLOSED', NULL),
('XASX', '2025-12-31', 'New Years Eve', 'EARLY_CLOSE', '14:10:00'),

-- 2026
('XASX', '2026-01-01', 'New Year''s Day', 'CLOSED', NULL),
('XASX', '2026-01-26', 'Australia Day', 'CLOSED', NULL),
('XASX', '2026-04-02', 'Day before Good Friday', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2026-04-03', 'Good Friday', 'CLOSED', NULL),
('XASX', '2026-04-06', 'Easter Monday', 'CLOSED', NULL),
('XASX', '2026-04-25', 'Anzac Day', 'CLOSED', NULL),
('XASX', '2026-06-08', 'King''s Birthday', 'CLOSED', NULL),
('XASX', '2026-12-24', 'Christmas Eve', 'EARLY_CLOSE', '14:10:00'),
('XASX', '2026-12-25', 'Christmas Day', 'CLOSED', NULL),
('XASX', '2026-12-28', 'Boxing Day (Substitute)', 'CLOSED', NULL),
('XASX', '2026-12-31', 'New Year''s Eve', 'EARLY_CLOSE', '14:10:00');

