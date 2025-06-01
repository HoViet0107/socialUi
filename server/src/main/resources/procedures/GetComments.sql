-- get comments of specific post
DELIMITER //
CREATE PROCEDURE GetComments(
    IN itemType VARCHAR(20),
    IN postId BIGINT,
    IN parentItemId BIGINT,
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
        fi.parent_item_id AS parentItemId,
        COALESCE(fi.post_id,0) as postId,
        COALESCE(fi.reply_to,0) as replyTo
    FROM feed_item fi
    JOIN feed_item_content fic ON fi.feed_item_id = fic.feed_item_id
    WHERE fi.f_status = 'ACTIVE'
      AND fi.item_type = itemType
      AND CASE
              WHEN parentItemId IS NULL THEN fi.parent_item_id IS NULL
              ELSE fi.parent_item_id = parentItemId
            END
      AND fi.post_id = postId
    ORDER BY fi.created_at DESC
    LIMIT pageSize OFFSET offsetValue;
END //
DELIMITER ;