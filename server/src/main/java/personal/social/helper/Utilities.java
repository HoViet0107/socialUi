package personal.social.helper;

import personal.social.model.Users;
import personal.social.repository.ConversationParticipantRepository;

import java.nio.file.AccessDeniedException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utilities {
    public static String buildFullName(Users user) {
        if (user == null) return "Unknown User";

        return buildFullName(user.getFirstName(), user.getSurname(), user.getLastName());
    }

    /**
     * Builds a full name string from the given first name, surname, and last name.
     *
     * <p>This method takes the three strings as input, removes leading and trailing spaces, and
     * drops empty strings. Finally, it joins the remaining strings with a space in between,
     * returning the resulting string.
     *
     * @param firstName  the first name
     * @param surname    the surname (middle name)
     * @param lastName   the last name
     * @return a full name string
     */
    public static String buildFullName(String firstName, String surname, String lastName) {
        String result = Stream.of(firstName, surname, lastName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));

        return result.isEmpty() ? "Unknown User" : result; // ✅ Default value
    }

    /**
     * Checks if a user has access to a specific conversation.
     *
     * <p>This method verifies if the user is a participant in the given conversation
     * by checking the participant repository. If the user is not a participant,
     * an AccessDeniedException is thrown.
     *
     * @param userId                the ID of the user to check
     * @param conversationId        the ID of the conversation to check access for
     * @param participantRepository the repository to use for checking participant data
     * @throws AccessDeniedException if the user is not a participant in the conversation
     */
    public static void isUserHasAccessToConversation(
            Long userId,
            Long conversationId,
            ConversationParticipantRepository participantRepository
    ) throws AccessDeniedException {
        if (participantRepository.findByUserInConversation(userId, conversationId) == null) {
            throw new AccessDeniedException("User is not a participant in this conversation!");
        }
    }
}
