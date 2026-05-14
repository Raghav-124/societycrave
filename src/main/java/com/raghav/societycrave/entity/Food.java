package com.raghav.societycrave.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(
    name = "food",
    uniqueConstraints = @UniqueConstraint(columnNames = {"name", "chefName", "chefFlatNumber", "societyName"})
)
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Food name is mandatory")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Chef name is mandatory")
    @Column(nullable = false)
    private String chefName;

    @NotBlank(message = "Chef flat number is mandatory")
    @Column(nullable = false)
    private String chefFlatNumber;

    private String chefCuisine;

    @NotBlank(message = "Society name is mandatory")
    @Column(nullable = false)
    private String societyName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;

    @NotNull(message = "Price is mandatory")
    @Column(nullable = false)
    private BigDecimal price;

    private String category;

    private String availableDays;

    private String openingTime;

    private String closingTime;

    private Integer slidingWindowMinutes;

    @Column(nullable = false)
    private boolean available = true;

    // ----------------------
    // Constructors
    // ----------------------
    public Food() {}

    public Food(Long id, String name, String chefName, String chefFlatNumber, String chefCuisine, String societyName,
                String description, BigDecimal price, String category, String availableDays, String openingTime,
                String closingTime, Integer slidingWindowMinutes, boolean available, String imageUrl) {
        this.id = id;
        this.name = name;
        this.chefName = chefName;
        this.chefFlatNumber = chefFlatNumber;
        this.chefCuisine = chefCuisine;
        this.societyName = societyName;
        this.description = description;
        this.price = price;
        this.category = category;
        this.availableDays = availableDays;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.slidingWindowMinutes = slidingWindowMinutes;
        this.available = available;
        this.imageUrl = imageUrl;
    }

    // ----------------------
    // Getters & Setters
    // ----------------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getChefName() { return chefName; }
    public void setChefName(String chefName) { this.chefName = chefName; }

    public String getChefFlatNumber() { return chefFlatNumber; }
    public void setChefFlatNumber(String chefFlatNumber) { this.chefFlatNumber = chefFlatNumber; }

    public String getChefCuisine() { return chefCuisine; }
    public void setChefCuisine(String chefCuisine) { this.chefCuisine = chefCuisine; }

    public String getSocietyName() { return societyName; }
    public void setSocietyName(String societyName) { this.societyName = societyName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAvailableDays() { return availableDays; }
    public void setAvailableDays(String availableDays) { this.availableDays = availableDays; }

    public String getOpeningTime() { return openingTime; }
    public void setOpeningTime(String openingTime) { this.openingTime = openingTime; }

    public String getClosingTime() { return closingTime; }
    public void setClosingTime(String closingTime) { this.closingTime = closingTime; }

    public Integer getSlidingWindowMinutes() { return slidingWindowMinutes; }
    public void setSlidingWindowMinutes(Integer slidingWindowMinutes) { this.slidingWindowMinutes = slidingWindowMinutes; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
