-- Insert 60 dummy question banks (2 per course)
-- Uses current schema: question_bank.course_id -> course.id
INSERT INTO question_bank (id, name, course_id, study_year) VALUES

-- Computer Science Courses
-- Introduction to Programming (Course ID: 201)
(301, 'Programming Fundamentals', 201, '2024-2025'),
(302, 'Java and Python Basics', 201, '2024-2025'),

-- Data Structures and Algorithms (Course ID: 202)
(303, 'Data Structures Fundamentals', 202, '2024-2025'),
(304, 'Algorithm Analysis', 202, '2024-2025'),

-- Object-Oriented Programming (Course ID: 203)
(305, 'OOP Principles', 203, '2024-2025'),
(306, 'Inheritance and Polymorphism', 203, '2024-2025'),

-- Database Management Systems (Course ID: 204)
(307, 'Relational Database Design', 204, '2024-2025'),
(308, 'SQL and Normalization', 204, '2024-2025'),

-- Software Engineering (Course ID: 205)
(309, 'Software Development Lifecycle', 205, '2024-2025'),
(310, 'Agile and Testing Methodologies', 205, '2024-2025'),

-- Web Development (Course ID: 206)
(311, 'Frontend Technologies', 206, '2024-2025'),
(312, 'Backend and Full-Stack', 206, '2024-2025'),

-- Computer Networks (Course ID: 207)
(313, 'Network Protocols', 207, '2024-2025'),
(314, 'TCP/IP and Distributed Systems', 207, '2024-2025'),

-- Machine Learning (Course ID: 208)
(315, 'ML Algorithms Fundamentals', 208, '2024-2025'),
(316, 'Supervised and Unsupervised Learning', 208, '2024-2025'),

-- Artificial Intelligence (Course ID: 209)
(317, 'AI Problem-Solving', 209, '2024-2025'),
(318, 'Search Algorithms and Logic', 209, '2024-2025'),

-- Cybersecurity Fundamentals (Course ID: 210)
(319, 'Security Principles', 210, '2024-2025'),
(320, 'Threat Detection and Mitigation', 210, '2024-2025'),

-- Mathematics Courses
-- Calculus I (Course ID: 211)
(321, 'Limits and Derivatives', 211, '2024-2025'),
(322, 'Basic Integration', 211, '2024-2025'),

-- Calculus II (Course ID: 212)
(323, 'Advanced Integration Techniques', 212, '2024-2025'),
(324, 'Infinite Series and Sequences', 212, '2024-2025'),

-- Linear Algebra (Course ID: 213)
(325, 'Vector Spaces and Operations', 213, '2024-2025'),
(326, 'Matrices and Linear Transformations', 213, '2024-2025'),

-- Discrete Mathematics (Course ID: 214)
(327, 'Logic and Set Theory', 214, '2024-2025'),
(328, 'Combinatorics and Graph Theory', 214, '2024-2025'),

-- Statistics and Probability (Course ID: 215)
(329, 'Probability Theory', 215, '2024-2025'),
(330, 'Statistical Analysis and Inference', 215, '2024-2025'),

-- Physics Courses
-- Classical Mechanics (Course ID: 216)
(331, 'Newtonian Mechanics', 216, '2024-2025'),
(332, 'Motion and Forces', 216, '2024-2025'),

-- Electromagnetism (Course ID: 217)
(333, 'Electric Field Theory', 217, '2024-2025'),
(334, 'Magnetic Fields and Maxwell Equations', 217, '2024-2025'),

-- Quantum Physics (Course ID: 218)
(335, 'Quantum Mechanics Fundamentals', 218, '2024-2025'),
(336, 'Wave-Particle Duality', 218, '2024-2025'),

-- Thermodynamics (Course ID: 219)
(337, 'Heat and Energy Transfer', 219, '2024-2025'),
(338, 'Statistical Mechanics', 219, '2024-2025'),

-- Business Courses
-- Principles of Management (Course ID: 220)
(339, 'Management Fundamentals', 220, '2024-2025'),
(340, 'Leadership and Organizational Behavior', 220, '2024-2025'),

-- Financial Accounting (Course ID: 221)
(341, 'Accounting Principles', 221, '2024-2025'),
(342, 'Financial Statements Analysis', 221, '2024-2025'),

-- Marketing Fundamentals (Course ID: 222)
(343, 'Consumer Behavior', 222, '2024-2025'),
(344, 'Marketing Mix and Strategies', 222, '2024-2025'),

-- Operations Management (Course ID: 223)
(345, 'Process Optimization', 223, '2024-2025'),
(346, 'Supply Chain Management', 223, '2024-2025'),

-- Strategic Management (Course ID: 224)
(347, 'Corporate Strategy', 224, '2024-2025'),
(348, 'Competitive Analysis', 224, '2024-2025'),

-- Liberal Arts Courses
-- English Literature (Course ID: 225)
(349, 'Classical Literature', 225, '2024-2025'),
(350, 'Modern Authors and Literary Analysis', 225, '2024-2025'),

-- World History (Course ID: 226)
(351, 'Ancient Civilizations', 226, '2024-2025'),
(352, 'Modern Historical Developments', 226, '2024-2025'),

-- Philosophy and Ethics (Course ID: 227)
(353, 'Moral Reasoning', 227, '2024-2025'),
(354, 'Philosophical Schools of Thought', 227, '2024-2025'),

-- Psychology Introduction (Course ID: 228)
(355, 'Basic Psychological Principles', 228, '2024-2025'),
(356, 'Research Methods in Psychology', 228, '2024-2025'),

-- Sociology Fundamentals (Course ID: 229)
(357, 'Social Structures', 229, '2024-2025'),
(358, 'Human Behavior and Society', 229, '2024-2025'),

-- Environmental Science (Course ID: 230)
(359, 'Ecology and Ecosystems', 230, '2024-2025'),
(360, 'Sustainability and Environmental Issues', 230, '2024-2025');
