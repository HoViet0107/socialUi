-- (like-unlike, dislike-undislike, share, report) procedure
DELIMITER //
CREATE PROCEDURE ToggleReaction(
    IN p_user_id BIGINT,
    IN p_object_type VARCHAR(50),
    IN p_object_id BIGINT,
    IN p_reaction_type VARCHAR(50)
)
BEGIN
    DECLARE count_reaction INT DEFAULT 0;

    SELECT COUNT(*) INTO count_reaction
    FROM user_reaction
    WHERE user_id = p_user_id AND object_type = p_object_type AND object_id = p_object_id AND reaction_type = p_reaction_type ;

    IF count_reaction > 0 THEN
        -- If existed -> delete it
        DELETE FROM user_reaction
        WHERE user_id = p_user_id AND reaction_type = p_reaction_type AND object_type = p_object_type AND object_id = p_object_id;
        -- update nums of like, dislike, share, report in reaction_aggregate
        IF p_reaction_type = 'LIKE' THEN
            UPDATE reaction_aggregate
            SET likes = GREATEST(likes - 1, 0)
            WHERE object_type = p_object_type AND object_id = p_object_id ;
        ELSEIF p_reaction_type = 'DISLIKE' THEN
            UPDATE reaction_aggregate
            SET dislikes = GREATEST(dislikes - 1, 0)
            WHERE object_type = p_object_type AND object_id = p_object_id ;
        ELSEIF p_reaction_type = 'REPORT' THEN
            UPDATE reaction_aggregate
            SET reports = GREATEST(reports - 1, 0)
            WHERE object_type = p_object_type AND object_id = p_object_id ;
        END IF;
    ELSE
        -- If not existed -> insert new
        INSERT INTO user_reaction(user_id, object_type, reaction_type, object_id)
        VALUES (p_user_id, p_object_type, p_reaction_type, p_object_id);
        -- increase like, dislike, share, report in reaction_aggregate
        IF p_reaction_type = 'LIKE' THEN
            UPDATE reaction_aggregate
            SET likes = likes + 1
            WHERE object_type = p_object_type AND object_id = p_object_id ;
        ELSEIF p_reaction_type = 'DISLIKE' THEN
            UPDATE reaction_aggregate
            SET dislikes = dislikes + 1
            WHERE object_type = p_object_type AND object_id = p_object_id ;
        ELSEIF p_reaction_type = 'REPORT' THEN
            UPDATE reaction_aggregate
            SET reports = reports + 1
            WHERE object_type = p_object_type AND object_id = p_object_id ;
        END IF;
    END IF;
END //
DELIMITER ;