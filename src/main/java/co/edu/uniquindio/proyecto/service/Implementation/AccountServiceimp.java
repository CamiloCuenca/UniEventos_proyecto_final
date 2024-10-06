package co.edu.uniquindio.proyecto.service.Implementation;

import co.edu.uniquindio.proyecto.Enum.AccountStatus;
import co.edu.uniquindio.proyecto.Enum.CouponStatus;
import co.edu.uniquindio.proyecto.Enum.Rol;
import co.edu.uniquindio.proyecto.Enum.TypeCoupon;
import co.edu.uniquindio.proyecto.config.JWTUtils;
import co.edu.uniquindio.proyecto.dto.Account.*;
import co.edu.uniquindio.proyecto.dto.Coupon.CouponDTO;
import co.edu.uniquindio.proyecto.dto.EmailDTO;
import co.edu.uniquindio.proyecto.dto.JWT.TokenDTO;
import co.edu.uniquindio.proyecto.exception.account.*;
import co.edu.uniquindio.proyecto.model.Accounts.Account;
import co.edu.uniquindio.proyecto.model.Accounts.User;
import co.edu.uniquindio.proyecto.model.Accounts.ValidationCode;
import co.edu.uniquindio.proyecto.model.Accounts.ValidationCodePassword;
import co.edu.uniquindio.proyecto.repository.AccountRepository;
import co.edu.uniquindio.proyecto.repository.CouponRepository;
import co.edu.uniquindio.proyecto.service.Interfaces.AccountService;
import co.edu.uniquindio.proyecto.service.Interfaces.CouponService;
import co.edu.uniquindio.proyecto.service.Interfaces.EmailService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.AccountNotFoundException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountServiceimp implements AccountService {

    private final AccountRepository cuentaRepo;

    private final PasswordEncoder passwordEncoder;

    private final JWTUtils jwtUtils;

    private final AccountRepository accountRepository;

    private  final CouponService couponService;

    private final EmailService emailService;

    /**
     * Metodo encargado validar que el Email no este en uso
     *
     * @param email
     * @return
     */
    private boolean existsEmail(String email) {
        return cuentaRepo.findByEmail(email).isPresent();
    }

    /**
     * Metodo para encriptar la contrasena.
     *
     * @param password
     * @return
     */
    private String encryptPassword(String password) {
        return passwordEncoder.encode(password);
    }

    /**
     * Metodo encargado de validar que la cedula del usuario no este en uso.
     *
     * @param idNumber
     * @return
     */
    private boolean idExists(String idNumber) {
        return cuentaRepo.findByIdnumber(idNumber).isPresent();
    }

    /**
     * Metodo encargado de crear el cuerpo del token.
     *
     * @param account
     * @return
     */
    private Map<String, Object> construirClaims(Account account) {
        return Map.of(
                "rol", account.getRol(),
                "nombre", account.getUser().getName(),
                "id", account.getAccountId()
        );
    }

    /**
     * Método login(): Permite iniciar sesión utilizando las credenciales de correo y contraseña del usuario. Si la cuenta no está activa o la contraseña es incorrecta, lanza las excepciones correspondientes.
     * Utiliza JWT para generar un token basado en los detalles de la cuenta (rol, nombre, id).
     *
     * @param loginDTO
     * @return
     * @throws EmailNotFoundException
     * @throws InvalidPasswordException
     */
    @Override
    public TokenDTO login(LoginDTO loginDTO) throws EmailNotFoundException, InvalidPasswordException, ActiveAccountException {
        Optional<Account> optionalAccount = cuentaRepo.findByEmail(loginDTO.email());

        // Corregido: lanzar excepción si no se encuentra el email
        if (optionalAccount.isEmpty()) {
            throw new EmailNotFoundException(loginDTO.email());
        }

        Account account = optionalAccount.get();
        if (!account.getStatus().equals(AccountStatus.ACTIVE)) {
            throw new ActiveAccountException("La cuenta no se encutra Activada. Porfavor activala para ingresar!");
        }

        // Verificar si la contraseña es válida
        if (!passwordEncoder.matches(loginDTO.password(), account.getPassword())) {
            throw new InvalidPasswordException();
        }


        Map<String, Object> map = construirClaims(account);



        return new TokenDTO(jwtUtils.generateToken(account.getEmail(), map));
    }

    /**
     * Método createAccount(): Crea una nueva cuenta, encripta la contraseña con BCryptPasswordEncoder, genera un código de validación, y envía un correo electrónico al usuario para activar la cuenta.
     * Valida que el correo y el número de identificación no existan previamente en la base de datos.
     *
     * @param cuenta
     * @return
     * @throws Exception
     */
    @Override
    public String createAccount(createAccountDTO cuenta) throws Exception {

        if (existsEmail(cuenta.email())) {
            throw new EmailAlreadyExistsException(cuenta.email());
        }

        if (idExists(cuenta.idNumber())) {
            throw new CedulaAlreadyExistsException(cuenta.idNumber());
        }
        //Segmento del codigo que se encarga de encriptar el codigo.
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = bCryptPasswordEncoder.encode(cuenta.password());

        //Mapeamos (pasamos) los datos del DTO a un objeto de tipo Cuenta
        Account newAccount = new Account();
        newAccount.setEmail(cuenta.email());
        //Clave encriptada.
        newAccount.setPassword(hashedPassword);
        newAccount.setRol(Rol.CUSTOMER);
        newAccount.setRegistrationDate(LocalDateTime.now());
        newAccount.setUser(new User(
                cuenta.idNumber(),
                cuenta.name(),
                cuenta.phoneNumber(),
                cuenta.address()
        ));
        newAccount.setStatus(AccountStatus.INACTIVE);

        String validationCode = generateValidationCode();
        ValidationCode validationCodeObj = new ValidationCode(validationCode);
        newAccount.setRegistrationValidationCode(validationCodeObj);

        Account createdAccount = cuentaRepo.save(newAccount);

        // Enviar el codigo de validacion por correo
        emailService.sendCodevalidation(createdAccount.getEmail(),validationCode);


        return createdAccount.getAccountId();
    }

    /**
     * Metodo encargado de generar un codigo de validacion.
     *
     * @return
     */
    private String generateValidationCode() {
        return UUID.randomUUID().toString().substring(0, 8); // Código de 8 caracteres
    }

    /**
     * Metodo encargado de actualizar o editar los datos de la cuenta.
     *
     * @param cuenta
     * @return
     * @throws Exception
     */
    @Override
    public String editAccount(editAccountDTO cuenta , String id) throws AccountNtFoundException {
        Optional<Account> optionalAccount = cuentaRepo.findById(id);

        if (optionalAccount.isEmpty()) {

            throw new AccountNtFoundException(id);
        }

        Account cuentaActualizada = optionalAccount.get();
        cuentaActualizada.getUser().setName(cuenta.username());
        cuentaActualizada.getUser().setPhoneNumber(cuenta.phoneNumber());
        cuentaActualizada.getUser().setAddress(cuenta.address());
        String newPassword = cuenta.password();
        if (newPassword != null && !newPassword.isEmpty()) {
            cuentaActualizada.setPassword(encryptPassword(newPassword));
        }
        cuentaRepo.save(cuentaActualizada);
        return cuentaActualizada.getAccountId();
    }

    /**
     * Método obtainAccountInformation(): Recupera la información básica de la cuenta basada en su ID.
     *
     * @param id
     * @return
     * @throws Exception
     */
    @Override
    public dtoAccountInformation obtainAccountInformation(String id) throws AccountNotFoundException {
        if (id == null) {
            throw new IllegalArgumentException("El ID no debe ser null");
        }

        Optional<Account> optionalCuenta = cuentaRepo.findById(id);
        if (optionalCuenta.isEmpty()) {
            throw new AccountNotFoundException(id);
        }
        Account account = optionalCuenta.get();

        return new dtoAccountInformation(
                account.getAccountId(),
                account.getUser().getIdNumber(),
                account.getUser().getPhoneNumber(),
                account.getUser().getAddress(),
                account.getEmail()
        );
    }

    /**
     * Método listAccounts(): Devuelve una lista de todas las cuentas registradas en la base de datos, lanzando una excepción si no se encuentran cuentas.
     *
     * @return
     */
    @Override
    public List<dtoAccountItem> listAccounts() {
        try {
            List<Account> cuentas = cuentaRepo.findAll();
            if (cuentas.isEmpty()) {
                throw new NoAccountsFoundException();
            }
            List<dtoAccountItem> items = new ArrayList<>();

            for (Account account : cuentas) {
                items.add(new dtoAccountItem(
                        account.getAccountId(),
                        account.getUser().getName(),
                        account.getEmail(),
                        account.getUser().getPhoneNumber()
                ));
            }
            return items;
        } catch (Exception e) {
            throw new AccountRetrievalException(e.getMessage());
        }
    }

    /**
     * Método deleteAccount(): Cambia el estado de una cuenta a "ELIMINADA" en lugar de borrarla físicamente.
     *
     * @param id
     * @return
     * @throws Exception
     */
    @Override
    public String deleteAccount(String id) throws AccountNotFoundException {
        //Buscamos la cuenta del usuario que se quiere eliminar
        Optional<Account> optionalCuenta = cuentaRepo.findById(id);
        if (optionalCuenta.isEmpty()) {
            throw new AccountNotFoundException(id);
        }
        Account cuenta = optionalCuenta.get();
        cuenta.setStatus(AccountStatus.ELIMINATED);
        cuentaRepo.save(cuenta);
        return cuenta.getAccountId();
    }

    /**
     * Método sendPasswordRecoveryCode(): Genera un código de validación para recuperar la contraseña y lo envía al correo electrónico del usuario.
     *
     * @param email
     * @return
     * @throws Exception
     */
    @Override
    public String sendPasswordRecoveryCode(String email) throws Exception {
        Optional<Account> optionalAccount = cuentaRepo.findByEmail(email);
        if (optionalAccount.isEmpty()) {
            throw new EmailNotFoundException(email);
        }
        Account account = optionalAccount.get();

        String passwordValidationCode = generateValidationCode();
        ValidationCodePassword validationCodePassword = new ValidationCodePassword(generateValidationCode());
        account.setPasswordValidationCode(validationCodePassword);
        cuentaRepo.save(account);

        // Enviar por email el codigo de recuperaciòn
        emailService.sendRecoveryCode(account.getEmail(),passwordValidationCode);

        return "Código de recuperación enviado al correo " + account.getEmail();
    }

    /**
     * Método changePassword(): Permite al usuario cambiar su contraseña tras verificar que el código de recuperación es válido y no ha expirado.
     *
     * @param changePasswordDTO
     * @return
     * @throws Exception
     */

    @Override
    public String changePassword(changePasswordDTO changePasswordDTO, String correo) throws Exception {
        Optional<Account> optionalAccount = cuentaRepo.findByEmail(correo);
        if (optionalAccount.isEmpty()) {
            throw new EmailNotFoundException(correo);
        }

        Account account = optionalAccount.get();
        ValidationCodePassword validationCodePassword = account.getPasswordValidationCode();

        if (validationCodePassword == null || validationCodePassword.isExpired()) {
            throw new ValidationCodeExpiredException();
        }

        if (!validationCodePassword.getCode().equals(changePasswordDTO.code())) {
            throw new InvalidValidationCodeException();
        }

        // Verificar que las contraseñas ingresadas coinciden
        if (!changePasswordDTO.newPassword().equals(changePasswordDTO.confirmationPassword())) {
            throw new PasswordsDoNotMatchException("Las contraseñas no coinciden.");
        }

        // Encriptar la nueva contraseña
        String encryptedPassword = passwordEncoder.encode(changePasswordDTO.newPassword());
        account.setPassword(encryptedPassword);

        // Limpiar el código de recuperación para evitar su reutilización
        account.setPasswordValidationCode(null);
        // Guardar la cuenta actualizada
        cuentaRepo.save(account);


        return "La contraseña ha sido cambiada exitosamente.";
    }

    /**
     * Método activateAccount(): Activa la cuenta de un usuario utilizando el código de validación que se envía por correo al momento de crear la cuenta.
     *
     * @param correo
     * @param code
     * @return
     * @throws Exception
     */
    @Override
    public String activateAccount(String correo, String code) throws Exception {
        // Buscar la cuenta por correo
        Account account = cuentaRepo.findByEmail(correo)
                .orElseThrow(() -> new EmailNotFoundException(correo));

        // Verificar si la cuenta ya está activada
        if (account.getStatus().equals(AccountStatus.ACTIVE)) {
            throw new AccountAlreadyActiveException("La cuenta ya está activada.");
        }

        // Obtener el código de validación de registro
        ValidationCode validationCode = account.getRegistrationValidationCode();

        if (validationCode == null) {
            throw new ValidationCodeExpiredException("El código de validación no existe.");
        }

        // Verificar si el código ha expirado
        if (validationCode.isExpired()) {
            throw new ValidationCodeExpiredException("El código de validación ha expirado.");
        }

        // Verificar si el código es válido
        if (!validationCode.getCode().equals(code)) {
            throw new InvalidValidationCodeException("El código de validación es inválido.");
        }


        // Crear el cupón usando el servicio de cupones
        CouponDTO couponDTO =  generateWelcomeCoupon();
        couponService.createCoupon(couponDTO);

        // Enviar por email el cupon de bienvenida
        emailService.sendWelcomeCoupon(account.getEmail(),couponDTO.code());

        // Activar la cuenta y eliminar el código de validación
        account.setRegistrationValidationCode(null);
        account.setStatus(AccountStatus.ACTIVE);
        cuentaRepo.save(account);

        return "Cuenta activada exitosamente y cupón enviado por correo";
    }




    /**
     *  Mètodo para generar el cupon de bienvenida de un 15% de descuento
     * @return cupon de bienvenida
     */
    public CouponDTO generateWelcomeCoupon() throws Exception {
       // Creamos el cupon de 15% de descueto
        CouponDTO couponDTO = new CouponDTO(
                "Cupón de Bienvenida",              // Nombre del cupón
                CouponService.generateRandomCouponCode(),         // Código único de cupón
                "15",                               // Descuento del 15%
                LocalDateTime.now().plusDays(30),   // Fecha de expiración (30 días a partir de ahora)
                CouponStatus.AVAILABLE,             // Estado disponible
                TypeCoupon.ONLY,                     // Tipo de cupón: uso único
                null,                                 // ID del evento: (en este caso no es requerido)
                LocalDateTime.now()                 // Fecha de inicio: hoy
        );
        return couponDTO;
    }


}
