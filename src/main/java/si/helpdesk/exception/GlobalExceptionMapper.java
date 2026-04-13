package si.helpdesk.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper {

    @Provider
    public static class ConversationNotFoundMapper implements ExceptionMapper<ConversationNotFoundException> {
        @Override
        public Response toResponse(ConversationNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("CONVERSATION_NOT_FOUND", e.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class UnauthorizedAccessMapper implements ExceptionMapper<UnauthorizedAccessException> {
        @Override
        public Response toResponse(UnauthorizedAccessException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse("FORBIDDEN", e.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class InvalidStateMapper implements ExceptionMapper<InvalidStateException> {
        @Override
        public Response toResponse(InvalidStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(e.errorCode, e.getMessage()))
                    .build();
        }
    }
}
