package co.edu.uniquindio.proyecto.service.Implementation;

import co.edu.uniquindio.proyecto.Enum.CouponStatus;
import co.edu.uniquindio.proyecto.Enum.TypeCoupon;
import co.edu.uniquindio.proyecto.dto.Coupon.CouponDTO;
import co.edu.uniquindio.proyecto.model.Coupons.Coupon;
import co.edu.uniquindio.proyecto.model.Events.Event;
import co.edu.uniquindio.proyecto.model.PurchaseOrder.Order;
import co.edu.uniquindio.proyecto.repository.CouponRepository;
import co.edu.uniquindio.proyecto.repository.EventRepository;
import co.edu.uniquindio.proyecto.repository.OrderRepository;
import co.edu.uniquindio.proyecto.service.Interfaces.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CouponServiceImp implements CouponService {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;

    /**
     * Metodo para crear un cupon.
     * @param couponDTO Data Transfer Object containing coupon details
     * @return
     * @throws Exception
     */
    @Override
    public String createCoupon(CouponDTO couponDTO) throws Exception {
        Coupon newCoupon = new Coupon();
        newCoupon.setName(couponDTO.name());
        newCoupon.setCode(generateRandomCouponCode());
        newCoupon.setDiscount(couponDTO.discount());
        newCoupon.setExpirationDate(couponDTO.expirationDate());
        newCoupon.setStatus(couponDTO.status());
        newCoupon.setType(couponDTO.type());

        // Asignar el evento si es que se especificó un eventId
        if (couponDTO.eventId() != null) {
            newCoupon.setEventId(couponDTO.eventId());
        }

        if (couponDTO.startDate() != null && couponDTO.expirationDate() != null) {
            if (couponDTO.startDate().isAfter(couponDTO.expirationDate())) {
                throw new Exception("La fecha de inicio no puede ser posterior a la fecha de fin.");
            }
        }

        if (couponDTO.eventId() != null) {
            Event event = eventRepository.findById(couponDTO.eventId())
                    .orElseThrow(() -> new Exception("El evento especificado no existe."));
            newCoupon.setEventId(event.getId());
        }

        // Asignar las fechas de inicio y fin si es que se especificaron
        if (couponDTO.startDate() != null) {
            newCoupon.setStartDate(couponDTO.startDate());
        }

        Coupon createdCoupon = couponRepository.save(newCoupon);
        return createdCoupon.getCouponId();
    }

    /**
     * Metodo para validar que cupon exista.
     * @param code Coupon code
     * @return
     * @throws Exception
     */
    @Override
    public boolean validateCoupon(String code) throws Exception {
        Coupon coupon = couponRepository.findByCode(code);
        if(coupon == null || coupon.getStatus() != CouponStatus.AVAILABLE){
            throw new Exception("El coupon no existe o no esta activo");
        }


        return true;
    }

    /**
     * Metodo para aplicar el cupon
     * @param code Coupon code
     * @param orderId The ID of the order to apply the coupon to
     * @return
     * @throws Exception
     */
    @Override
    public double applyCoupon(String code, String orderId) throws Exception {
        // Validar si el cupón es válido o ya expiró
        if (!validateCoupon(code)) {
            throw new Exception("El cupón es inválido o ya expiró");
        }

        // Buscar el cupón por su código
        Coupon coupon = couponRepository.findByCode(code);

        // Verificar el tipo de cupón (UNICO o MULTIPLE)
        if (coupon.getType() == TypeCoupon.ONLY && coupon.getStatus() == CouponStatus.NOT_AVAILABLE) {
            throw new Exception("El cupón ya ha sido utilizado");
        }

        // Buscar la orden por su ID
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new Exception("La orden no existe"));

        // Aplicar el cupón a la orden
        double discount = applyDiscountToOrder(order, coupon);

        // Marcar el cupón como usado si es UNICO
        if (coupon.getType() == TypeCoupon.ONLY) {
            coupon.setStatus(CouponStatus.NOT_AVAILABLE);
            couponRepository.save(coupon);
        }

        // Guardar la orden actualizada
        orderRepository.save(order);

        // Retornar el valor del descuento aplicado
        return discount;
    }

    @Override
    public List<Coupon> getAvailableCoupons() {
        return couponRepository.findAvailableCoupons();
    }

    /**
     * Metodo para desactivar el cupon.
     * @param couponId The ID of the coupon to deactivate
     * @throws Exception
     */
    @Override
    public void deactivateCoupon(String couponId) throws Exception {
        // Buscar el cupón por su ID y lanzar una excepción si no existe
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new Exception("El cupón no existe"));

        // Cambiar el estado del cupón a NO_DISPONIBLE
        coupon.setStatus(CouponStatus.NOT_AVAILABLE);

        // Guardar el cupón actualizado en la base de datos
        couponRepository.save(coupon);
    }

    /**
     * Metodo para activar el cupon
     * @param couponId
     * @throws Exception
     */
    @Override
    public void activateCoupon(String couponId) throws Exception {
        // Buscar el cupón por su ID y lanzar una excepción si no existe
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new Exception("El cupón no existe"));
        // Cambiar el estado del cupón a DISPONIBLE
        coupon.setStatus(CouponStatus.AVAILABLE);

        // Guardar el cupón actualizado en la base de datos
        couponRepository.save(coupon);

    }


    /**
     * Metodo que se encarga de actualizar el cupon.
     * @param couponId The ID of the coupon to update
     * @param couponDTO The new coupon information to update
     * @throws Exception
     */
    @Override
    public void updateCoupon(String couponId, CouponDTO couponDTO) throws Exception {
        Optional<Coupon> optionalCoupon = couponRepository.findById(couponId);

        if (optionalCoupon.isPresent()) {
            throw new Exception("El coupon no existe");
        }

        Coupon coupon = optionalCoupon.get();
        coupon.setName(couponDTO.name());
        coupon.setCode(couponDTO.code());
        coupon.setDiscount(couponDTO.discount());
        coupon.setExpirationDate(couponDTO.expirationDate());
        coupon.setStatus(couponDTO.status());

        couponRepository.save(coupon);
    }

    /**
     * Metodo que se encarga de aplicar un descuento a la orden.
     * @param order
     * @param coupon
     * @return
     */
    private double applyDiscountToOrder(Order order, Coupon coupon) {
        // Lógica para calcular el descuento basado en el tipo de cupón
        double  discountPercentage = Integer.parseInt(coupon.getDiscount())/ 100.0;

        // Calcular el monto del descuento
        double discountAmount = order.getTotal() * discountPercentage;

        // Actualizar el total de la orden
        order.setTotal(order.getTotal() - discountAmount);


        return discountAmount;
    }

    // Método auxiliar para generar un código de cupón aleatorio
    public String generateRandomCouponCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();  // Código aleatorio de 8 caracteres
    }

}
