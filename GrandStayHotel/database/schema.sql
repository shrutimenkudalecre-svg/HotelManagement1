-- Grand Stay Hotel Management System
-- Database Schema

CREATE DATABASE IF NOT EXISTS grand_stay_hotel;
USE grand_stay_hotel;

-- Admin table (fixed credentials, no signup)
CREATE TABLE IF NOT EXISTS admin (
    id INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) DEFAULT 'Admin'
);

-- Insert fixed admin credentials (password: Admin@123)
INSERT INTO admin (email, password, name) VALUES 
('admin@grandstayhotel.com', 'Admin@123', 'Grand Stay Admin')
ON DUPLICATE KEY UPDATE email = email;

-- Customers table
CREATE TABLE IF NOT EXISTS customers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    mobile VARCHAR(15),
    security_question VARCHAR(200),
    security_answer VARCHAR(200),
    is_approved TINYINT(1) DEFAULT 0,  -- 0=Pending, 1=Approved, 2=Rejected
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Rooms table
CREATE TABLE IF NOT EXISTS rooms (
    id INT PRIMARY KEY AUTO_INCREMENT,
    room_number VARCHAR(10) UNIQUE NOT NULL,
    room_type ENUM('AC', 'NON AC', 'Deluxe', 'Suite') NOT NULL,
    bed_type ENUM('Single', 'Double', 'King') NOT NULL,
    price_per_day DECIMAL(10,2) NOT NULL,
    status ENUM('Available', 'Booked', 'Maintenance') DEFAULT 'Available'
);

-- Insert sample rooms
INSERT INTO rooms (room_number, room_type, bed_type, price_per_day, status) VALUES
('101', 'AC', 'Single', 1500.00, 'Available'),
('102', 'AC', 'Double', 2500.00, 'Available'),
('103', 'NON AC', 'Single', 800.00, 'Available'),
('104', 'NON AC', 'Double', 1200.00, 'Available'),
('201', 'Deluxe', 'King', 4000.00, 'Available'),
('202', 'Deluxe', 'Double', 3500.00, 'Available'),
('301', 'Suite', 'King', 7000.00, 'Available'),
('302', 'Suite', 'King', 8000.00, 'Available'),
('103', 'AC', 'Single', 250.00, 'Booked') ON DUPLICATE KEY UPDATE room_number = room_number;

-- Customer Check-in records
CREATE TABLE IF NOT EXISTS checkins (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_name VARCHAR(100) NOT NULL,
    mobile VARCHAR(15) NOT NULL,
    email VARCHAR(100),
    gender ENUM('Male', 'Female', 'Other'),
    nationality VARCHAR(50),
    aadhar_number VARCHAR(20),
    address TEXT,
    room_id INT,
    checkin_date DATE NOT NULL,
    checkout_date DATE,
    price_per_day DECIMAL(10,2),
    total_amount DECIMAL(10,2),
    status ENUM('Checked In', 'Checked Out') DEFAULT 'Checked In',
    FOREIGN KEY (room_id) REFERENCES rooms(id)
);

-- Booking requests (from customer portal)
CREATE TABLE IF NOT EXISTS booking_requests (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_email VARCHAR(100) NOT NULL,
    customer_name VARCHAR(100),
    room_type VARCHAR(50),
    checkin_date DATE,
    checkout_date DATE,
    num_guests INT DEFAULT 1,
    total_cost DECIMAL(10,2) DEFAULT 0,
    paid_amount DECIMAL(10,2) DEFAULT 0,
    status ENUM('Pending', 'Confirmed', 'Cancelled') DEFAULT 'Pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bills table
CREATE TABLE IF NOT EXISTS bills (
    id INT PRIMARY KEY AUTO_INCREMENT,
    checkin_id INT,
    room_number VARCHAR(10),
    customer_name VARCHAR(100),
    mobile VARCHAR(15),
    nationality VARCHAR(50),
    gender VARCHAR(10),
    checkin_date DATE,
    checkout_date DATE,
    num_days INT,
    total_amount DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (checkin_id) REFERENCES checkins(id)
);

-- Chat messages
CREATE TABLE IF NOT EXISTS chat_messages (
    id INT PRIMARY KEY AUTO_INCREMENT,
    guest_email VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    sender ENUM('Guest', 'Admin') DEFAULT 'Guest',
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Booking confirmations
CREATE TABLE IF NOT EXISTS booking_confirmations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT,
    customer_email VARCHAR(100),
    room_number VARCHAR(10),
    pending_payment DECIMAL(10,2),
    description TEXT,
    confirmed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES booking_requests(id)
);
