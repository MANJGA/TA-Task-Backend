package models;

import lombok.Data;
import java.util.List;

@Data
public class UsersResponse {
    private List<User> users;
}