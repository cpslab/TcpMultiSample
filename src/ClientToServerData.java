import java.io.Serializable;
import java.util.Date;

public record ClientToServerData(String message, Date date) implements Serializable {
}
