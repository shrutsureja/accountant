-- PIN for development users is 123456. This deterministic PBKDF2 value is for local setup only.
INSERT INTO users (id, username, display_name, pin_hash, avatar_initials, created_at, updated_at) VALUES
('user-shrut', 'shrut', 'Shrut', 'pbkdf2:310000:ZGV2LXNlZWQtc2FsdA==:qUFePm/yPr+fglRAeliwzX+paYRTjM7ALeoDxIaJQm0=', 'S', datetime('now'), datetime('now')),
('user-mom', 'mom', 'Mom', 'pbkdf2:310000:ZGV2LXNlZWQtc2FsdA==:qUFePm/yPr+fglRAeliwzX+paYRTjM7ALeoDxIaJQm0=', 'M', datetime('now'), datetime('now')),
('user-dad', 'dad', 'Dad', 'pbkdf2:310000:ZGV2LXNlZWQtc2FsdA==:qUFePm/yPr+fglRAeliwzX+paYRTjM7ALeoDxIaJQm0=', 'D', datetime('now'), datetime('now'));

INSERT INTO categories (id, name, icon, created_at, updated_at) VALUES
('cat-groceries','Groceries','shopping_cart',datetime('now'),datetime('now')),
('cat-food','Food & Dining','restaurant',datetime('now'),datetime('now')),
('cat-household','Household','home',datetime('now'),datetime('now')),
('cat-shopping','Shopping','shopping_bag',datetime('now'),datetime('now')),
('cat-fuel','Fuel','local_gas_station',datetime('now'),datetime('now')),
('cat-transport','Transport','directions_car',datetime('now'),datetime('now')),
('cat-medical','Medical','medical_services',datetime('now'),datetime('now')),
('cat-utilities','Utilities','bolt',datetime('now'),datetime('now')),
('cat-entertainment','Entertainment','movie',datetime('now'),datetime('now')),
('cat-education','Education','school',datetime('now'),datetime('now')),
('cat-travel','Travel','flight',datetime('now'),datetime('now')),
('cat-care','Personal Care','spa',datetime('now'),datetime('now')),
('cat-gifts','Gifts','redeem',datetime('now'),datetime('now')),
('cat-investment','Investment','trending_up',datetime('now'),datetime('now')),
('cat-other','Other','category',datetime('now'),datetime('now'));

INSERT INTO accounts (id, name, bank_name, owner_user_id, payment_method, created_at, updated_at) VALUES
('account-cash','Cash',NULL,NULL,'CASH',datetime('now'),datetime('now'));
