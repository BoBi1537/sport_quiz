-- 20251031002135-initial-schema.up.sql

CREATE TABLE question (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_id VARCHAR(50) NOT NULL,
    question_data JSON NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
--;;
CREATE INDEX idx_question_game_id ON question(game_id);
--;;
CREATE TABLE game_session (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mode VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    session_data JSON DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
--;;


CREATE TABLE player_session (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_session_id INT NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    is_host BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (game_session_id) REFERENCES game_session(id) ON DELETE CASCADE
);
--;;
CREATE INDEX idx_player_session_session_id ON player_session(game_session_id);
--;;
CREATE TABLE session_answer (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_session_id INT NOT NULL,
    question_id INT,
    chosen_answer VARCHAR(255),
    is_correct BOOLEAN,
    score_gained INT DEFAULT 0,
    answered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_session_id) REFERENCES player_session(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE SET NULL
);