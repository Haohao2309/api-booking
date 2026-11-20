INSERT INTO locations (name, description) VALUES
    ('Đà Nẵng', 'Biển Mỹ Khê, cầu Rồng và trung tâm thành phố'),
    ('Hà Nội', 'Phố cổ, hồ Hoàn Kiếm và khu vực trung tâm'),
    ('Đà Lạt', 'Không khí mát mẻ, gần hồ Xuân Hương'),
    ('Phú Quốc', 'Biển đảo, resort và các hoạt động nghỉ dưỡng')
ON CONFLICT (name) DO NOTHING;

INSERT INTO rooms (number, name, type, description, capacity, price_per_night, active, location_id)
SELECT v.number, v.name, v.type, v.description, v.capacity, v.price, TRUE, l.id
FROM (VALUES
    ('DN-101', 'Ocean View Apartment', 'Căn hộ biển', 'Căn hộ hướng biển, ban công rộng và bếp riêng.', 2, 1680000, 'Đà Nẵng'),
    ('DN-202', 'My Khe Deluxe Room', 'Phòng Deluxe', 'Phòng Deluxe gần biển Mỹ Khê, phù hợp cho cặp đôi.', 2, 1250000, 'Đà Nẵng'),
    ('DN-303', 'Dragon Bridge Family Suite', 'Suite gia đình', 'Suite rộng, có phòng khách và tầm nhìn thành phố.', 4, 2450000, 'Đà Nẵng'),
    ('HN-101', 'Old Quarter Studio', 'Studio', 'Studio hiện đại trong khu phố cổ Hà Nội.', 2, 1350000, 'Hà Nội'),
    ('HN-204', 'Hoan Kiem Boutique Room', 'Phòng Boutique', 'Phòng boutique yên tĩnh, đi bộ đến hồ Hoàn Kiếm.', 2, 1580000, 'Hà Nội'),
    ('HN-305', 'West Lake Family Apartment', 'Căn hộ gia đình', 'Căn hộ đầy đủ tiện nghi, phù hợp nhóm nhỏ.', 4, 2200000, 'Hà Nội'),
    ('DL-101', 'Pine Hill Cabin', 'Cabin', 'Nhà gỗ giữa đồi thông, có sân hiên riêng.', 4, 1950000, 'Đà Lạt'),
    ('DL-202', 'Xuan Huong Garden Room', 'Phòng Garden', 'Phòng nghỉ gần hồ Xuân Hương, không gian xanh mát.', 2, 1100000, 'Đà Lạt'),
    ('DL-303', 'Misty Valley Villa', 'Villa', 'Villa riêng tư với bếp và khu BBQ ngoài trời.', 6, 3600000, 'Đà Lạt'),
    ('PQ-101', 'Sunset Villa', 'Villa biển', 'Villa nghỉ dưỡng gần biển, hồ bơi riêng.', 6, 5200000, 'Phú Quốc'),
    ('PQ-202', 'Long Beach Bungalow', 'Bungalow', 'Bungalow sân vườn, cách bãi biển vài phút đi bộ.', 2, 2100000, 'Phú Quốc'),
    ('PQ-303', 'Island Family Suite', 'Suite gia đình', 'Suite rộng cho gia đình, có khu bếp tiện lợi.', 4, 3250000, 'Phú Quốc')
) AS v(number, name, type, description, capacity, price, location_name)
JOIN locations l ON l.name = v.location_name
ON CONFLICT (number) DO NOTHING;
