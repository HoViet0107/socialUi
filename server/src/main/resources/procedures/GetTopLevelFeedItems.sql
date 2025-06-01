DELIMITER //
CREATE PROCEDURE GetTopLevelFeedItems(
    IN pageNumber INT,
    IN pageSize INT
)
BEGIN
    DECLARE offsetValue INT;
    SET offsetValue = (pageNumber - 1) * pageSize;

    SELECT
        fi.feed_item_id AS feedItemId,
        fi.created_at AS createdAt,
        fi.updated_at AS updatedAt,
        fi.f_status AS feedItemStatus,
        fi.item_type AS itemType,
        fic.content,
        fi.user_id AS userId,
        COALESCE(fi.parent_item_id, 0) AS parentItemId,
        COALESCE(fi.post_id,0) as postId,
        COALESCE(fi.reply_to,0) as replyTo
    FROM feed_item fi
    JOIN feed_item_content fic ON fi.feed_item_id = fic.feed_item_id
    WHERE fi.f_status = 'ACTIVE'
      AND fi.item_type = 'POST'
      AND (fi.parent_item_id IS NULL OR fi.parent_item_id = 0)
    ORDER BY fi.created_at DESC
    LIMIT pageSize OFFSET offsetValue;
END //
DELIMITER ;