package si.helpdesk.room;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Arrays;
import java.util.List;

@Tag(name = "Rooms")
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    @RolesAllowed("USER")
    @Operation(summary = "List rooms", description = "Returns all available help desk rooms.")
    @APIResponse(responseCode = "200", description = "List of rooms")
    @APIResponse(responseCode = "401", description = "Not authenticated")
    public List<String> listRooms() {
        return Arrays.stream(Room.values()).map(Enum::name).toList();
    }
}
