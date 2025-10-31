-- 20251031002135-initial-schema.up.sql

CREATE TABLE question (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_id VARCHAR(50) NOT NULL,
    question_data TEXT NOT NULL
);
--;;

CREATE TABLE game_session (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mode VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_game_index INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
--;;

CREATE TABLE player_session (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    total_score INT DEFAULT 0,
    is_host BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (session_id) REFERENCES game_session(id) ON DELETE CASCADE
);
--;;

CREATE TABLE session_answer (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_session_id INT NOT NULL,
    question_id INT NOT NULL,
    chosen_answer VARCHAR(255),
    is_correct BOOLEAN,
    score_gained INT DEFAULT 0,
    answered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_session_id) REFERENCES player_session(id) ON DELETE CASCADE
);
