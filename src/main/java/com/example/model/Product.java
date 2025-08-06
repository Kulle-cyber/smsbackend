package com.example.model;

public class Product {
    private Integer id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String imageUrl;
    private Integer salespersonId; // renamed from sellerId

    // Constructor
    public Product(Integer id, String name, String description, Double price, Integer stock, String imageUrl, Integer salespersonId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.salespersonId = salespersonId;
    }

    // Getters and setters
    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Double getPrice() { return price; }
    public Integer getStock() { return stock; }
    public String getImageUrl() { return imageUrl; }
    public Integer getSalespersonId() { return salespersonId; }

    public void setId(Integer id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(Double price) { this.price = price; }
    public void setStock(Integer stock) { this.stock = stock; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setSalespersonId(Integer salespersonId) { this.salespersonId = salespersonId; }
}
