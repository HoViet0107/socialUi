import { FeedItemServices, UserServices } from "src/services/api";

/**
 * Fetches user details for a given feed item based on its `userId`.
 * 
 * - Sends a request to `UserServices.getUserById(userId)` to get user information.
 * - If successful, attaches the returned user data to the feed item under `feedItem.user`.
 * - Returns the updated feed item object for further use.
 * - Logs any errors encountered during the fetch process.
 * 
 * @param {Object} feedItem - The feed item object that contains a `userId`.
 * @returns {Object} The same feed item object with an additional `user` field (if fetch succeeds).
 */
export async function fetchUserDetails(feedItem) {
    try {
        const response = await UserServices.getUserById(feedItem.userId);
        if (response.data) {
            feedItem.user = response.data;
        }
        return feedItem;
    } catch (error) {
        console.error("Failed to fetch user: ", error);
    }
}

/**
 * Fetches a list of feed items based on the provided request body.
 *
 * This function calls the `getFeedItems` method from the FeedItemServices API
 * and returns the list of feed items if the response contains embedded data.
 *
 * @param {Object} requestBody - The request payload containing query parameters such as item type, pagination info, filters, etc.
 * @returns {Promise<Array<Object>|undefined>} A promise that resolves to an array of feed items, or undefined if an error occurs or data is not found.
 */
export async function fetchFeedItemList(requestBody) {
    try {
        const response = await FeedItemServices.getFeedItems(requestBody);
        if (response.data._embedded && response.data._embedded.feedItemDTOList) {
            return response.data._embedded.feedItemDTOList;
        }
    } catch (error) {
        console.error(`Failed to fetch feed item: ${requestBody.itemType}`, error);
    }
}

export async function fetchFeedItemById() {}

/**
 * Fetches reaction counts for a specific feed item.
 * 
 * @param {Object} feedItem - The feed item object containing a feedItemId.
 * @param {string} token - The authentication token for the API call.
 * @returns {Promise<Object>} The feed item with reaction data attached.
 */
export async function fetchFeedItemReactionCount(feedItem, token) {
    try {
        const response = await FeedItemServices.getFeedItemReactionCount(feedItem.feedItemId, token);
        if (response.data) {
            feedItem.reactions = response.data;
        }
        return feedItem;
    } catch (error) {
        console.error("Failed to fetch feed item reaction count:", error);
    }
}

/**
 * Sends a request to create a new feed item (comment or reply) using the provided data and token.
 *
 * @async
 * @function createFeedItem
 * @param {Object} data - The data payload containing `content`, `itemType`, and `parentItemId`.
 * @param {string} token - The authentication token for the API call.
 * @returns {Promise<Object|undefined>} Returns the created feed item data if successful, otherwise `undefined`.
 */
export async function createFeedItem(data, token) {
    try {
        if (!token) {
            console.error('No authentication token found');
            return;
        }
        // Call the API to create a new feed item
        const response = await FeedItemServices.createFeedItem(data, token);

        return response.data;
    } catch (error) {
        console.error("Failed to create feed item:", error);
    }
}


/**
 * Sends a request to update a feed item via the API.
 *
 * @param {Object} data - The request payload containing the feed item update.
 * @param {string} token - The authentication token for the API request.
 * @returns {Promise<Object|undefined>} - The API response, or undefined if an error occurs.
 */
export async function editFeedItem(data, token) {
    try {
        if (!token) {
            console.error('No authentication token found');
            return;
        }

        // Call the API to edit the feed item
        return await FeedItemServices.editFeedItem(data, token);
    } catch (error) {
        console.error("Failed to edit feed item:", error);
    }
}

/**
 * Deletes a feed item (post or comment) after confirming with the user.
 * 
 * - Prompts a confirmation dialog.
 * - Validates the presence of an authentication token.
 * - Sends an API request to mark the item as deleted (soft delete).
 * - Removes the item from the local `feedItems` array to update the UI.
 *
 * @param {Ref} feedItems - A Vue Ref object holding the array of feed items.
 * @param {Object} feedItem - The item to be deleted.
 * @param {Object} data - The payload containing itemId, itemType, and new status.
 * @param {Object} token - The authentication token required by the API.
 * @returns {Array|undefined} - The updated list of feed items or undefined if deletion is cancelled/failed.
 */
export async function deleteFeedItem(feedItems, feedItem, data, token) {
    if (confirm(`Are you sure you want to delete this ${feedItem.itemType}?`)) {
        try {
            if (!token) {
                console.error('No authentication token found');
                return;
            }

            // Call the API to delete the feed item
            await FeedItemServices.editFeedItem(data, token);
            // Remove the feed item from the local array
            console.log("Deleting feed item:", feedItems);

            feedItems.value = feedItems.value.filter(
                fi => fi.feedItemId !== feedItem.feedItemId
            );
        } catch (error) {
            console.error("Failed to delete:", error);
        }
    }
}