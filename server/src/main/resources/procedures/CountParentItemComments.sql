DELIMITER //
CREATE PROCEDURE CountParentItemComments(
    IN parentItemId BIGINT
)
BEGIN
    SELECT COUNT(*) AS totalCount
    FROM feed_item fi
    WHERE fi.f_status = 'ACTIVE'
      AND fi.item_type = 'COMMENT'
      AND fi.parent_item_id = parentItemId;
END //
DELIMITER ;
