package uk.gov.pay.webhooks.publicapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Size;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class Address {

    @Size(max = 255, message = "Must be less than or equal to {max} characters length")
    private String line1;

    @Size(max = 255, message = "Must be less than or equal to {max} characters length")
    private String line2;

    @Size(max = 25, message = "Must be less than or equal to {max} characters length")
    private String postcode;

    @Size(max = 255, message = "Must be less than or equal to {max} characters length")
    private String city;

    private String country;

    public Address(@JsonProperty("line1") String line1,
                   @JsonProperty("line2") String line2,
                   @JsonProperty("postcode") String postcode,
                   @JsonProperty("city") String city,
                   @JsonProperty("country") String country) {
        this.line1 = line1;
        this.line2 = line2;
        this.postcode = postcode;
        this.city = city;
        this.country = country;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(line1, address.line1) &&
                Objects.equals(line2, address.line2) &&
                Objects.equals(postcode, address.postcode) &&
                Objects.equals(city, address.city) &&
                Objects.equals(country, address.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(line1, line2, postcode, city, country);
    }
}
