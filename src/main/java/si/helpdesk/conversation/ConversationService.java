package si.helpdesk.conversation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import si.helpdesk.exception.ConversationNotFoundException;
import si.helpdesk.exception.InvalidStateException;
import si.helpdesk.exception.UnauthorizedAccessException;
import si.helpdesk.message.*;
import si.helpdesk.operator.Operator;
import si.helpdesk.user.User;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class ConversationService {

    @Inject
    MessageBroadcaster broadcaster;

    @Transactional
    public ConversationDTO createConversation(Long userId, CreateConversationRequest request) {
        User user = User.findById(userId);
        if (user == null) {
            throw new UnauthorizedAccessException("User not found");
        }

        Conversation conv = new Conversation();
        conv.room = request.room;
        conv.user = user;
        conv.status = ConversationStatus.PENDING;
        conv.createdAt = Instant.now();
        conv.updatedAt = Instant.now();
        conv.persist();

        Message msg = new Message();
        msg.conversation = conv;
        msg.senderType = SenderType.USER;
        msg.senderId = userId;
        msg.content = request.message;
        msg.sentAt = Instant.now();
        msg.persist();

        return ConversationDTO.from(conv, request.message);
    }

    public ConversationDTO getConversation(Long conversationId, Long userId) {
        Conversation conv = Conversation.findByIdAndUser(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        Message first = Message.findFirstByConversation(conversationId);
        return ConversationDTO.from(conv, first != null ? first.content : null);
    }

    @Transactional
    public MessageDTO sendUserMessage(Long conversationId, Long userId, SendMessageRequest request) {
        Conversation conv = Conversation.findByIdAndUser(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        if (conv.status == ConversationStatus.PENDING) {
            throw new InvalidStateException("CONVERSATION_NOT_ACTIVE",
                    "Cannot send a message to a conversation that is not active.");
        }
        if (conv.status == ConversationStatus.CLOSED) {
            throw new InvalidStateException("CONVERSATION_CLOSED",
                    "Cannot send a message to a closed conversation.");
        }

        Message msg = new Message();
        msg.conversation = conv;
        msg.senderType = SenderType.USER;
        msg.senderId = userId;
        msg.content = request.content;
        msg.sentAt = Instant.now();
        msg.persist();

        MessageDTO dto = MessageDTO.from(msg);
        broadcaster.broadcast(conversationId, dto);
        return dto;
    }

    public List<MessageDTO> getMessages(Long conversationId, Long userId, Long since) {
        Conversation.findByIdAndUser(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        List<Message> messages = since != null
                ? Message.findByConversationSince(conversationId, since)
                : Message.findByConversation(conversationId);
        return messages.stream().map(MessageDTO::from).toList();
    }

    // Operator methods

    public List<ConversationDTO> listConversations(ConversationStatus status) {
        List<Conversation> conversations = status != null
                ? Conversation.findByStatus(status)
                : Conversation.listAll();
        return conversations.stream().map(c -> {
            Message first = Message.findFirstByConversation(c.id);
            return ConversationDTO.from(c, first != null ? first.content : null);
        }).toList();
    }

    @Transactional
    public ConversationDTO takeConversation(Long conversationId, Long operatorId) {
        Conversation conv = Conversation.findById(conversationId);
        if (conv == null) {
            throw new ConversationNotFoundException(conversationId);
        }
        if (conv.status != ConversationStatus.PENDING) {
            throw new InvalidStateException("CONVERSATION_NOT_PENDING",
                    "Conversation is not in PENDING state.");
        }

        Operator operator = Operator.findById(operatorId);
        conv.operator = operator;
        conv.status = ConversationStatus.ACTIVE;
        conv.updatedAt = Instant.now();
        conv.persist();

        Message first = Message.findFirstByConversation(conversationId);
        return ConversationDTO.from(conv, first != null ? first.content : null);
    }

    @Transactional
    public MessageDTO sendOperatorMessage(Long conversationId, Long operatorId, SendMessageRequest request) {
        Conversation conv = Conversation.findById(conversationId);
        if (conv == null) {
            throw new ConversationNotFoundException(conversationId);
        }
        if (conv.status != ConversationStatus.ACTIVE) {
            throw new InvalidStateException("CONVERSATION_NOT_ACTIVE",
                    "Cannot send a message to a conversation that is not active.");
        }
        if (conv.operator == null || !conv.operator.id.equals(operatorId)) {
            throw new UnauthorizedAccessException("You are not assigned to this conversation.");
        }

        Message msg = new Message();
        msg.conversation = conv;
        msg.senderType = SenderType.OPERATOR;
        msg.senderId = operatorId;
        msg.content = request.content;
        msg.sentAt = Instant.now();
        msg.persist();

        MessageDTO dto = MessageDTO.from(msg);
        broadcaster.broadcast(conversationId, dto);
        return dto;
    }

    public List<MessageDTO> getOperatorMessages(Long conversationId, Long operatorId, Long since) {
        Conversation conv = Conversation.findById(conversationId);
        if (conv == null) {
            throw new ConversationNotFoundException(conversationId);
        }
        if (conv.operator == null || !conv.operator.id.equals(operatorId)) {
            throw new UnauthorizedAccessException("You are not assigned to this conversation.");
        }

        List<Message> messages = since != null
                ? Message.findByConversationSince(conversationId, since)
                : Message.findByConversation(conversationId);
        return messages.stream().map(MessageDTO::from).toList();
    }

    @Transactional
    public ConversationDTO closeConversation(Long conversationId, Long operatorId) {
        Conversation conv = Conversation.findById(conversationId);
        if (conv == null) {
            throw new ConversationNotFoundException(conversationId);
        }
        if (conv.status != ConversationStatus.ACTIVE) {
            throw new InvalidStateException("CONVERSATION_NOT_ACTIVE",
                    "Conversation is not in ACTIVE state.");
        }
        if (conv.operator == null || !conv.operator.id.equals(operatorId)) {
            throw new UnauthorizedAccessException("You are not assigned to this conversation.");
        }

        conv.status = ConversationStatus.CLOSED;
        conv.updatedAt = Instant.now();
        conv.persist();

        Message first = Message.findFirstByConversation(conversationId);
        return ConversationDTO.from(conv, first != null ? first.content : null);
    }
}
