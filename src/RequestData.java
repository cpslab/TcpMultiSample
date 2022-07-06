import java.io.Serializable;
import java.util.Date;

public record RequestData(String message, Date date) implements Serializable {
}
