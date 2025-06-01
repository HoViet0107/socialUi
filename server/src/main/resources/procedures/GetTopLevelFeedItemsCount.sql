DELIMITER //
CREATE PROCEDURE GetTopLevelFeedItemsCount(
    IN fItemType VARCHAR(20)
)
BEGIN
    SELECT COUNT(fi.feed_item_id) AS totalCount
    FROM feed_item fi
    WHERE fi.f_status = 'ACTIVE'
      AND fi.item_type = fItemType
      AND (fi.parent_item_id IS NULL OR fi.parent_item_id = 0);
END //
DELIMITER ;
