package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.Enum.AccountStatus;
import co.edu.uniquindio.proyecto.model.Accounts.Account;
import co.edu.uniquindio.proyecto.model.Accounts.User;
import co.edu.uniquindio.proyecto.model.Accounts.ValidationCodePassword;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends MongoRepository<Account, String> {
    Optional<Account> findByEmail(String email);

    @Query("{'_id': ?0}")
    Optional<Account> findByIdnumber(String accountId);

    long countByStatus(AccountStatus status);

    @Query(value = "{ '_id' : ?0 }", fields = "{ 'email' : 1 }")
    Optional<Account> findEmailById(String accountId);

}

