package practicum.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("accounts")
@Builder
public class Account {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("balance")
    private BigDecimal balance;
}
