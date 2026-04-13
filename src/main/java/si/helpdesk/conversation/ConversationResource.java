package si.helpdesk.conversation;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.json.JsonNumber;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import si.helpdesk.message.MessageBroadcaster;
import si.helpdesk.message.MessageDTO;
import si.helpdesk.message.SendMessageRequest;

import java.util.List;

@Tag(name = "User Conversations")
@Path("/conversations")
@RolesAllowed("USER")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConversationResource {

    @Inject
    ConversationService conversationService;

    @Inject
    MessageBroadcaster broadcaster;

    @Inject
    JsonWebToken jwt;

    private Long getUserId() {
        JsonNumber n = jwt.getClaim("userId");
        return n.longValue();
    }

    @GET
    @Operation(summary = "List my conversations",
               description = "Returns all conversations belonging to the authenticated user, newest first.")
    @APIResponse(responseCode = "200", description = "List of conversations")
    public List<ConversationDTO> listConversations() {
        return conversationService.listUserConversations(getUserId());
    }

    @POST
    @Operation(summary = "Start a new conversation",
               description = "Creates a new conversation in the given room with an initial message.")
    @APIResponse(responseCode = "201", description = "Conversation created")
    @APIResponse(responseCode = "400", description = "Invalid request")
    @APIResponse(responseCode = "401", description = "Not authenticated")
    public Response createConversation(@Valid CreateConversationRequest request) {
        ConversationDTO dto = conversationService.createConversation(getUserId(), request);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get conversation details",
               description = "Returns the status and details of a specific conversation.")
    @APIResponse(responseCode = "200", description = "Conversation details")
    @APIResponse(responseCode = "404", description = "Conversation not found")
    public ConversationDTO getConversation(@PathParam("id") Long id) {
        return conversationService.getConversation(id, getUserId());
    }

    @POST
    @Path("/{id}/messages")
    @Operation(summary = "Send a message",
               description = "Sends a message in an ACTIVE conversation. Returns 409 if PENDING or CLOSED.")
    @APIResponse(responseCode = "201", description = "Message sent")
    @APIResponse(responseCode = "409", description = "Conversation not active")
    public Response sendMessage(@PathParam("id") Long id, @Valid SendMessageRequest request) {
        MessageDTO dto = conversationService.sendUserMessage(id, getUserId(), request);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @GET
    @Path("/{id}/messages")
    @Operation(summary = "Get messages",
               description = "Returns all messages in a conversation. Use ?since={messageId} for polling.")
    @APIResponse(responseCode = "200", description = "List of messages")
    public List<MessageDTO> getMessages(@PathParam("id") Long id,
                                        @QueryParam("since") Long since) {
        return conversationService.getMessages(id, getUserId(), since);
    }

    @GET
    @Path("/{id}/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Blocking
    @Operation(summary = "SSE stream for incoming messages",
               description = "Server-Sent Events stream that pushes new messages as they arrive.")
    @APIResponse(responseCode = "200", description = "SSE stream opened")
    @APIResponse(responseCode = "403", description = "Not your conversation")
    public Multi<MessageDTO> stream(@PathParam("id") Long id) {
        // Verify the user owns this conversation (throws if not)
        conversationService.getConversation(id, getUserId());
        return Multi.createFrom().emitter(emitter -> broadcaster.register(id, emitter));
    }
}
