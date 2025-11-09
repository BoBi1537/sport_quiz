-- 20251031002135-initial-schema.down.sql

DROP TABLE IF EXISTS session_answer;
--;;
DROP TABLE IF EXISTS player_session;
--;;
DROP INDEX IF EXISTS idx_player_session_session_id ON player_session;
--;;
DROP TABLE IF EXISTS game_session;
--;;
DROP INDEX IF EXISTS idx_question_game_id ON question;
--;;
DROP TABLE IF EXISTS question;