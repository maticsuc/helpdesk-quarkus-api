package si.helpdesk.auth;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import si.helpdesk.exception.ErrorResponse;

@Tag(name = "Auth")
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/login/user")
    @Operation(summary = "User login", description = "Authenticates a user and returns a JWT token.")
    @APIResponse(responseCode = "200", description = "Login successful, JWT returned")
    @APIResponse(responseCode = "401", description = "Invalid credentials")
    public Response loginUser(@Valid LoginRequest request) {
        try {
            return Response.ok(authService.loginUser(request.username, request.password)).build();
        } catch (AuthService.UnauthorizedLoginException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("INVALID_CREDENTIALS", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/login/operator")
    @Operation(summary = "Operator login", description = "Authenticates an operator and returns a JWT token.")
    @APIResponse(responseCode = "200", description = "Login successful, JWT returned")
    @APIResponse(responseCode = "401", description = "Invalid credentials")
    public Response loginOperator(@Valid LoginRequest request) {
        try {
            return Response.ok(authService.loginOperator(request.username, request.password)).build();
        } catch (AuthService.UnauthorizedLoginException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("INVALID_CREDENTIALS", e.getMessage()))
                    .build();
        }
    }
}
