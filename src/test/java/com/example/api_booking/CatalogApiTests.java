package com.example.api_booking;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Public catalog: no login needed, filtering and paging happen on the server. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogApiTests {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;

  long hotel;
  long otherHotel;
  long standard;
  long family;
  long standardRoom;
  long familyRoom;
  long lockedRoom;
  long bookedRoom;
  final LocalDate checkIn = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(10);
  final LocalDate checkOut = checkIn.plusDays(2);

  private long insert(String sql, Object... args) {
    var key = new GeneratedKeyHolder();
    db.update(
        c -> {
          var s = c.prepareStatement(sql, new String[] {"id"});
          for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
          return s;
        },
        key);
    return key.getKey().longValue();
  }

  private long hotel(String name) {
    return insert(
        "insert into hotels(name,address,contact_email,phone,timezone,check_in_time,check_out_time)"
            + " values(?,?,?,?,?,?,?)",
        name,
        "Hà Nội",
        "hotel@example.com",
        "0901234567",
        "Asia/Ho_Chi_Minh",
        java.sql.Time.valueOf("14:00:00"),
        java.sql.Time.valueOf("12:00:00"));
  }

  private long type(long hotelId, String name, int capacity, String price, boolean active) {
    return insert(
        "insert into room_types(hotel_id,name,description,capacity,base_price,active)"
            + " values(?,?,?,?,?,?)",
        hotelId,
        name,
        name + " mô tả",
        capacity,
        new java.math.BigDecimal(price),
        active);
  }

  private long room(String code, long typeId, boolean locked) {
    // Legacy price/capacity columns differ on purpose: the room type is the source of truth.
    return insert(
        "insert into rooms(code,name,type,price,capacity,locked,room_type_id) values(?,?,?,?,?,?,?)",
        code,
        "Phòng " + code,
        "LEGACY",
        new java.math.BigDecimal("1"),
        1,
        locked,
        typeId);
  }

  @BeforeEach
  void seed() {
    String tag = Long.toString(System.nanoTime());
    hotel = hotel("Khách sạn A " + tag);
    otherHotel = hotel("Khách sạn B " + tag);
    standard = type(hotel, "Standard", 2, "1200000", true);
    family = type(hotel, "Family", 4, "2000000", true);
    long retired = type(hotel, "Retired", 2, "900000", false);
    standardRoom = room("S-" + tag, standard, false);
    familyRoom = room("F-" + tag, family, false);
    lockedRoom = room("L-" + tag, standard, true);
    bookedRoom = room("B-" + tag, standard, false);
    room("R-" + tag, retired, false);
    room("O-" + tag, type(otherHotel, "Standard", 2, "1000000", true), false);
    // bookedRoom is sold for the first night of the searched stay by a confirmed booking.
    long guest =
        insert(
            "insert into app_users(email,password_hash,role) values(?,?,'CUSTOMER')",
            "catalog-" + tag + "@example.com",
            "not-a-real-hash");
    long booking =
        insert(
            "insert into bookings(room_id,customer_id,room_name,customer_email,check_in,check_out,"
                + "guests,price_per_night,total_price,status,request_key,booking_code,hotel_id)"
                + " values(?,?,?,?,?,?,1,1200000,1200000,'CONFIRMED',?,?,?)",
            bookedRoom,
            guest,
            "Phòng B",
            "catalog-" + tag + "@example.com",
            checkIn,
            checkIn.plusDays(1),
            "seed-" + tag,
            "CAT-" + tag,
            hotel);
    long line =
        insert(
            "insert into booking_rooms(booking_id,room_id,room_label_snapshot,price_per_night,"
                + "number_of_nights,subtotal) values(?,?,?,1200000,1,1200000)",
            booking,
            bookedRoom,
            "Phòng B");
    db.update(
        "insert into room_inventory(room_id,stay_date,state,booking_room_id) values(?,?,'BOOKED',?)",
        bookedRoom,
        checkIn,
        line);
    db.update("insert into room_images(room_id,position,url) values(?,0,?)", standardRoom, "https://example.com/s.jpg");
    long wifi = insert("insert into amenities(name) values(?)", "Wi-Fi " + tag);
    db.update("insert into room_type_amenities(room_type_id,amenity_id) values(?,?)", standard, wifi);
    insert(
        "insert into hotel_services(hotel_id,name,unit_price,unit,active) values(?,?,?,?,?)",
        hotel, "Ăn sáng", new java.math.BigDecimal("150000"), "người", true);
    insert(
        "insert into hotel_services(hotel_id,name,unit_price,unit,active) values(?,?,?,?,?)",
        hotel, "Ngừng bán", new java.math.BigDecimal("1"), "lần", false);
    insert(
        "insert into nearby_suggestions(hotel_id,name,category,active) values(?,?,?,?)",
        hotel, "Hồ Gươm", "Tham quan", true);
    insert(
        "insert into nearby_suggestions(hotel_id,name,category,active) values(?,?,?,?)",
        otherHotel, "Chợ đêm", "Mua sắm", true);
  }

  @Test
  void searchFiltersOnTheServerWithoutLogin() throws Exception {
    // Locked rooms and inactive room types are never listed; prices come from the room type.
    mvc.perform(get("/api/v1/rooms").param("hotelId", Long.toString(hotel)).param("sort", "price,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalItems").value(3))
        .andExpect(jsonPath("$.data.items[*].id")
            .value(containsInAnyOrder((int) standardRoom, (int) bookedRoom, (int) familyRoom)))
        .andExpect(jsonPath("$.data.items[0].price").value(1200000))
        .andExpect(jsonPath("$.data.items[0].type").value("Standard"));

    // Whole-stay availability: the room sold on the first night drops out.
    mvc.perform(get("/api/v1/rooms")
            .param("hotelId", Long.toString(hotel))
            .param("checkIn", checkIn.toString())
            .param("checkOut", checkOut.toString()))
        .andExpect(jsonPath("$.data.totalItems").value(2))
        .andExpect(jsonPath("$.data.items[*].id")
            .value(containsInAnyOrder((int) standardRoom, (int) familyRoom)));
    // [checkIn, checkOut): a stay ending on that night's date does not collide.
    mvc.perform(get("/api/v1/rooms")
            .param("hotelId", Long.toString(hotel))
            .param("roomTypeId", Long.toString(standard))
            .param("checkIn", checkIn.minusDays(1).toString())
            .param("checkOut", checkIn.toString()))
        .andExpect(jsonPath("$.data.totalItems").value(2));

    mvc.perform(get("/api/v1/rooms").param("hotelId", Long.toString(hotel)).param("guests", "3"))
        .andExpect(jsonPath("$.data.items", hasSize(1)))
        .andExpect(jsonPath("$.data.items[0].id").value((int) familyRoom));
    mvc.perform(get("/api/v1/rooms")
            .param("hotelId", Long.toString(hotel))
            .param("maxPrice", "1500000")
            .param("size", "1"))
        .andExpect(jsonPath("$.data.size").value(1))
        .andExpect(jsonPath("$.data.items", hasSize(1)))
        .andExpect(jsonPath("$.data.totalItems").value(2))
        .andExpect(jsonPath("$.data.totalPages").value(2));
  }

  @Test
  void invalidSearchCriteriaAreRejected() throws Exception {
    mvc.perform(get("/api/v1/rooms").param("checkIn", checkIn.toString()))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/rooms")
            .param("checkIn", checkIn.toString())
            .param("checkOut", checkIn.toString()))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/rooms")
            .param("checkIn", LocalDate.now().minusDays(3).toString())
            .param("checkOut", LocalDate.now().minusDays(1).toString()))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/rooms").param("minPrice", "500").param("maxPrice", "100"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/rooms").param("guests", "0")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/rooms").param("size", "101")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/rooms").param("sort", "locked,asc")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/rooms").param("checkIn", "not-a-date"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void detailIncludesImagesAndAmenitiesButHidesLockedRooms() throws Exception {
    mvc.perform(get("/api/v1/rooms/" + standardRoom))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hotelId").value((int) hotel))
        .andExpect(jsonPath("$.data.roomTypeId").value((int) standard))
        .andExpect(jsonPath("$.data.capacity").value(2))
        .andExpect(jsonPath("$.data.images[0]").value("https://example.com/s.jpg"))
        .andExpect(jsonPath("$.data.amenities", hasSize(1)));
    mvc.perform(get("/api/v1/rooms/" + lockedRoom))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.data.code").value("RESOURCE_NOT_FOUND"));
    mvc.perform(get("/api/v1/rooms/999999999")).andExpect(status().isNotFound());
  }

  @Test
  void servicesAndSuggestionsListOnlyActiveEntriesOfTheHotel() throws Exception {
    mvc.perform(get("/api/v1/services").param("hotelId", Long.toString(hotel)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalItems").value(1))
        .andExpect(jsonPath("$.data.items[0].name").value("Ăn sáng"))
        .andExpect(jsonPath("$.data.items[0].unitPrice").value(150000));
    mvc.perform(get("/api/v1/suggestions").param("hotelId", Long.toString(hotel)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalItems").value(1))
        .andExpect(jsonPath("$.data.items[0].name").value("Hồ Gươm"));
    mvc.perform(get("/api/v1/services").param("hotelId", "0")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/suggestions").param("sort", "address,asc"))
        .andExpect(status().isBadRequest());
  }
}
