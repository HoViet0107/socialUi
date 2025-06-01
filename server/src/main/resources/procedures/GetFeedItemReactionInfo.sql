DELIMITER //
CREATE PROCEDURE GetFeedItemReactionInfo(
    IN userId BIGINT,
    IN feedItemId BIGINT
)
BEGIN
    DECLARE isLike BOOLEAN DEFAULT FALSE;
    DECLARE isDislike BOOLEAN DEFAULT FALSE;
    DECLARE isShare BOOLEAN DEFAULT FALSE;
    DECLARE isReport BOOLEAN DEFAULT FALSE;

    -- set isLike
    IF (SELECT COUNT(*) FROM user_reaction
        WHERE feed_item_id = feedItemId
          AND reaction_type = 'LIKE'
          AND user_id = userId) > 0
    THEN
        SET isLike = TRUE;
    END IF;

    -- set isDislike
    IF (SELECT COUNT(*) FROM user_reaction
        WHERE feed_item_id = feedItemId
          AND reaction_type = 'DISLIKE'
          AND user_id = userId) > 0
    THEN
        SET isDislike = TRUE;
    END IF;

    -- set isShare
    IF (SELECT COUNT(*) FROM user_reaction
        WHERE feed_item_id = feedItemId
          AND reaction_type = 'SHARE'
          AND user_id = userId) > 0
    THEN
        SET isShare = TRUE;
    END IF;

    -- set isReport
    IF (SELECT COUNT(*) FROM user_reaction
        WHERE feed_item_id = feedItemId
          AND reaction_type = 'REPORT'
          AND user_id = userId) > 0
    THEN
        SET isReport = TRUE;
    END IF;

    -- return both aggregate + state variables
    SELECT DISTINCT ra.likes,
           isLike AS isLike,
           ra.dislikes,
           isDislike AS isDislike,
           ra.shares,
           isShare AS isShare,
           ra.reports,
           isReport AS isReport
    FROM reaction_aggregate ra
    WHERE ra.feed_item_id = feedItemId;
END //
DELIMITER ;