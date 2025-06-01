package personal.social.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Lớp DTO dùng để đóng gói dữ liệu phân trang trả về cho client.
 *
 * @param <T> Kiểu dữ liệu của phần tử trong danh sách phân trang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse <T>{
    /**
     * Danh sách các phần tử thuộc trang hiện tại.
     */
    private List<T> content;

    /**
     * Số thứ tự của trang hiện tại (bắt đầu từ 0).
     */
    private int nextPage;

    /**
     * có còn phần từ nữa không
     */
    private boolean hasMore;
}
