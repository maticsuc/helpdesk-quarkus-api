package si.helpdesk.conversation;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonNumber;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import si.helpdesk.message.MessageBroadcaster;
import si.helpdesk.message.MessageDTO;
import si.helpdesk.message.SendMessageRequest;

import java.util.List;

@Tag(name = "Operator Conversations")
@Path("/operator/conversations")
@RolesAllowed("OPERATOR")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OperatorConversationResource {

    @Inject
    ConversationService conversationService;

    @Inject
    MessageBroadcaster broadcaster;

    @Inject
    JsonWebToken jwt;

    private Long getOperatorId() {
        JsonNumber n = jwt.getClaim("operatorId");
        return n.longValue();
    }

    @GET
    @Operation(summary = "List conversations",
               description = "Returns all conversations, optionally filtered by status (PENDING, ACTIVE, CLOSED).")
    @APIResponse(responseCode = "200", description = "List of conversations")
    public List<ConversationDTO> listConversations(@QueryParam("status") ConversationStatus status) {
        return conversationService.listConversations(status);
    }

    @POST
    @Path("/{id}/take")
    @Operation(summary = "Take a pending conversation",
               description = "Assigns the authenticated operator to a PENDING conversation and sets its status to ACTIVE.")
    @APIResponse(responseCode = "200", description = "Conversation successfully taken")
    @APIResponse(responseCode = "404", description = "Conversation not found")
    @APIResponse(responseCode = "409", description = "Conversation is not in PENDING state")
    public Response takeConversation(@PathParam("id") Long id) {
        ConversationDTO dto = conversationService.takeConversation(id, getOperatorId());
        return Response.ok(dto).build();
    }

    @POST
    @Path("/{id}/messages")
    @Operation(summary = "Send a message to user",
               description = "Sends a message in an ACTIVE conversation assigned to this operator.")
    @APIResponse(responseCode = "201", description = "Message sent")
    @APIResponse(responseCode = "403", description = "Not assigned to this conversation")
    @APIResponse(responseCode = "409", description = "Conversation not active")
    public Response sendMessage(@PathParam("id") Long id, @Valid SendMessageRequest request) {
        MessageDTO dto = conversationService.sendOperatorMessage(id, getOperatorId(), request);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @GET
    @Path("/{id}/messages")
    @Operation(summary = "Get messages",
               description = "Returns all messages in a conversation. Use ?since={messageId} for polling.")
    @APIResponse(responseCode = "200", description = "List of messages")
    public List<MessageDTO> getMessages(@PathParam("id") Long id,
                                        @QueryParam("since") Long since) {
        return conversationService.getOperatorMessages(id, getOperatorId(), since);
    }

    @GET
    @Path("/{id}/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Blocking
    @Operation(summary = "SSE stream for incoming messages",
               description = "Server-Sent Events stream that pushes new messages as they arrive.")
    @APIResponse(responseCode = "200", description = "SSE stream opened")
    @APIResponse(responseCode = "403", description = "Not assigned to this conversation")
    public Multi<MessageDTO> stream(@PathParam("id") Long id) {
        return Multi.createFrom().emitter(emitter -> broadcaster.register(id, emitter));
    }

    @PUT
    @Path("/{id}/close")
    @Operation(summary = "Close a conversation",
               description = "Sets an ACTIVE conversation to CLOSED. Only the assigned operator can close it.")
    @APIResponse(responseCode = "200", description = "Conversation closed")
    @APIResponse(responseCode = "403", description = "Not assigned to this conversation")
    @APIResponse(responseCode = "409", description = "Conversation is not ACTIVE")
    public Response closeConversation(@PathParam("id") Long id) {
        ConversationDTO dto = conversationService.closeConversation(id, getOperatorId());
        return Response.ok(dto).build();
    }
}
