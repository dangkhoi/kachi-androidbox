# Bảng đo tương phản — VISUAL-REFRESH P1 (§6.4)

> **Trạng thái**: Current · **Cập nhật**: sinh lại mỗi lượt `:app:testDebugUnitTest` · **Mục đích**: bảng đo tương phản VISUAL-REFRESH P1 §6.4 (sinh bằng máy — đừng sửa tay).

> SINH BẰNG MÁY từ `KachiPalette` bởi `SurfaceContrastContractTest.sinh bang do tuong phan cua tai lieu`.
> **Không sửa tay** — sửa bảng màu rồi chạy lại `:app:testDebugUnitTest`.

## Bảng TỐI

| Cặp | Vai | Sàn | Đo được | Kết |
|---|---|---|---|---|
| `INK trên surfFrom` | chữ chính, đỉnh gradient | 4.5 | **12.53** | ✅ |
| `INK trên surfTo` | chữ chính, đáy gradient | 4.5 | **15.73** | ✅ |
| `MUT trên surfFrom` | nhãn phụ, đỉnh | 4.5 | **6.11** | ✅ |
| `MUT trên surfTo` | nhãn phụ, đáy | 4.5 | **7.66** | ✅ |
| `MUT2 trên surfTo` | mực mờ nhất, đáy | 4.5 | **7.16** | ✅ |
| `INK trên fieldSunken` | chữ trong ô lõm | 4.5 | **15.25** | ✅ |
| `INK trên surfOnFrom` | chữ trên thẻ BẬT, đỉnh | 4.5 | **7.18** | ✅ |
| `INK trên surfOnTo` | chữ trên thẻ BẬT, đáy | 4.5 | **10.62** | ✅ |
| `surfLine trên surfFrom` + `surfFrom ÷ bg` | tách thẻ khỏi nền (**viền ≥ 3.0 HOẶC bước ≥ 1.15**) — ⚠ WP1: viền KHÔNG còn được vẽ, chỉ còn bước sáng trên màn | 3.0 / 1.15 | viền **2.16** · bước **1.19** | ✅ |
| `surfOnFrom ÷ surfFrom` | bước sáng BẬT↔thường | 1.2 | **1.75** | ✅ |
| `slot ÷ bg` + `lineStrong trên slot` | bậc 1 · khay ô làm việc trên nền màn (**bước ≥ 1.12 HOẶC viền ≥ 3.0**) | 1.12 / 3.0 | bước **1.01** · viền **3.16** | ✅ |
| `surfFrom ÷ slot` | bậc 2 · thẻ nội dung trên khay | 1.15 | **1.20** | ✅ |
| `surfFrom ÷ surfTo` | chuyển sắc DỌC = toàn bộ chiều nổi (bảng SÁNG miễn: đỉnh đã trắng) | 1.20 | **1.25** | ✅ |
| `INK trên slot` | chữ ô nhóm, đỉnh khay | 4.5 | **15.08** | ✅ |
| `MUT2 trên slotTo` | nhãn nhóm mờ nhất, đáy khay | 4.5 | **6.94** | ✅ |
| `lineStrong trên bg` | mốc cũ phải giữ | 3.0 | **3.17** | ✅ |
| `MUT (⇄ ô trống) trên đĩa kính, ảnh xấu nhất` | ô trống trong suốt: ⇄ là dấu duy nhất (OQ8 · B, L∈[0,1]) | 3.0 | **4.72** | ✅ |
| `ON_ACCENT trên gradFrom` | mốc cũ phải giữ | 4.5 | **4.83** | ✅ |
| `INK trên surfFromOverArt (ảnh sáng)` | P1b · chữ chính trên ảnh | 4.5 | **6.65** | ✅ |
| `MUT trên surfFromOverArt (ảnh sáng)` | P1b · nhãn phụ — CẦN scrim ở P1b | 4.5 | **3.24** | ❌ |
| `INK trên surfFromOverArt (ảnh tối)` | P1b · chữ chính trên ảnh | 4.5 | **14.06** | ✅ |
| `MUT trên surfFromOverArt (ảnh tối)` | P1b · nhãn phụ — CẦN scrim ở P1b | 4.5 | **6.85** | ✅ |

## Bảng SÁNG

| Cặp | Vai | Sàn | Đo được | Kết |
|---|---|---|---|---|
| `INK trên surfFrom` | chữ chính, đỉnh gradient | 4.5 | **18.17** | ✅ |
| `INK trên surfTo` | chữ chính, đáy gradient | 4.5 | **17.12** | ✅ |
| `MUT trên surfFrom` | nhãn phụ, đỉnh | 4.5 | **7.22** | ✅ |
| `MUT trên surfTo` | nhãn phụ, đáy | 4.5 | **6.81** | ✅ |
| `MUT2 trên surfTo` | mực mờ nhất, đáy | 4.5 | **6.03** | ✅ |
| `INK trên fieldSunken` | chữ trong ô lõm | 4.5 | **13.57** | ✅ |
| `INK trên surfOnFrom` | chữ trên thẻ BẬT, đỉnh | 4.5 | **10.41** | ✅ |
| `INK trên surfOnTo` | chữ trên thẻ BẬT, đáy | 4.5 | **11.86** | ✅ |
| `surfLine trên surfFrom` + `surfFrom ÷ bg` | tách thẻ khỏi nền (**viền ≥ 3.0 HOẶC bước ≥ 1.15**) — ⚠ WP1: viền KHÔNG còn được vẽ, chỉ còn bước sáng trên màn | 3.0 / 1.15 | viền **3.71** · bước **1.13** | ✅ |
| `surfOnFrom ÷ surfFrom` | bước sáng BẬT↔thường | 1.2 | **1.75** | ✅ |
| `slot ÷ bg` + `lineStrong trên slot` | bậc 1 · khay ô làm việc trên nền màn (**bước ≥ 1.12 HOẶC viền ≥ 3.0**) | 1.12 / 3.0 | bước **1.09** · viền **3.88** | ✅ |
| `surfFrom ÷ slot` | bậc 2 · thẻ nội dung trên khay | 1.15 | **1.23** | ✅ |
| `surfFrom ÷ surfTo` | chuyển sắc DỌC = toàn bộ chiều nổi (bảng SÁNG miễn: đỉnh đã trắng) | 1.20 | **1.06** | ✅ |
| `INK trên slot` | chữ ô nhóm, đỉnh khay | 4.5 | **14.80** | ✅ |
| `MUT2 trên slotTo` | nhãn nhóm mờ nhất, đáy khay | 4.5 | **4.78** | ✅ |
| `lineStrong trên bg` | mốc cũ phải giữ | 3.0 | **4.21** | ✅ |
| `MUT (⇄ ô trống) trên đĩa kính, ảnh xấu nhất` | ô trống trong suốt: ⇄ là dấu duy nhất (OQ8 · B, L∈[0,1]) | 3.0 | **5.07** | ✅ |
| `ON_ACCENT trên gradFrom` | mốc cũ phải giữ | 4.5 | **5.74** | ✅ |
| `INK trên surfFromOverArt (ảnh sáng)` | P1b · chữ chính trên ảnh | 4.5 | **18.17** | ✅ |
| `MUT trên surfFromOverArt (ảnh sáng)` | P1b · nhãn phụ — CẦN scrim ở P1b | 4.5 | **7.22** | ✅ |
| `INK trên surfFromOverArt (ảnh tối)` | P1b · chữ chính trên ảnh | 4.5 | **11.31** | ✅ |
| `MUT trên surfFromOverArt (ảnh tối)` | P1b · nhãn phụ — CẦN scrim ở P1b | 4.5 | **4.50** | ❌ |

