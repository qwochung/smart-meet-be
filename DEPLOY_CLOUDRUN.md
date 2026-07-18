# DEPLOY BE — Google Cloud Run

> Dễ nhất: dùng lại `Dockerfile`, HTTPS tự động (`*.run.app`), 1GB RAM (hết OOM).
> Kiến trúc: **FE (Vercel) → Cloud Run (HTTPS) → ASR**. Biến môi trường nạp từ `env.yaml`.

---

## Phần 1 — Tạo tài khoản & project

1. Vào [cloud.google.com](https://cloud.google.com) → **Get started for free** → kích hoạt **$300 credit / 90 ngày** (cần thẻ verify, không tự trừ tiền).
2. Console → tạo **Project** mới, ví dụ tên `smart-meet`. Ghi lại **Project ID**.

---

## Phần 2 — Mở Cloud Shell (khỏi cài gcloud)

Góc trên phải Console → bấm icon **Cloud Shell** (`>_`). Đây là terminal có sẵn `gcloud` + Docker + git, khỏi cài gì trên máy.

```bash
gcloud config set project <PROJECT_ID>
```

---

## Phần 3 — Lấy code + env.yaml

```bash
git clone <URL_REPO> smart-meet
cd smart-meet/smart-meet-be
```

`env.yaml` bị `.gitignore` nên KHÔNG có trong repo → tạo lại trên Cloud Shell:
```bash
nano env.yaml     # dán nội dung env.yaml (bản có secret bạn giữ ở máy)
```

---

## Phần 4 — Deploy (1 lệnh)

Chạy trong thư mục `smart-meet-be`:
```bash
gcloud run deploy smart-meet-be \
  --source . \
  --region asia-southeast1 \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 1 \
  --timeout 3600 \
  --port 8080 \
  --allow-unauthenticated \
  --env-vars-file env.yaml
```

- Lần đầu sẽ hỏi **bật API** (Cloud Run, Cloud Build, Artifact Registry) → gõ `y`.
- Nó tự build `Dockerfile` → deploy. Chờ ~3–5 phút.
- Xong in ra **Service URL**: `https://smart-meet-be-xxxxxxxx.asia-southeast1.run.app`.

**Giải thích flag quan trọng:**
| Flag | Vì sao |
|---|---|
| `--memory 1Gi` | Đủ RAM, hết OOM như Render |
| `--max-instances 1` | Broker STOMP in-memory → chỉ 1 instance cho WebSocket khỏi lệch |
| `--timeout 3600` | Giữ kết nối WebSocket lâu (mặc định 300s sẽ rớt) |
| `--allow-unauthenticated` | Cho phép public truy cập |
| `--region asia-southeast1` | Singapore, gần VN nhất |

---

## Phần 5 — Trỏ FE về BE mới

Trên **Vercel → Settings → Environment Variables**, sửa (thay URL run.app thật):
```
VITE_API_URL = https://smart-meet-be-xxxxxxxx.asia-southeast1.run.app/api
VITE_WS_URL  = wss://smart-meet-be-xxxxxxxx.asia-southeast1.run.app/api/ws/meet
```
→ **Redeploy** FE (env Vite là build-time).

Và cập nhật `CLIENT_URL` trong `env.yaml` = URL Vercel (đã đúng sẵn), rồi deploy lại BE nếu cần.

---

## Phần 6 — Vận hành / cập nhật

| Việc | Lệnh (trong smart-meet-be) |
|---|---|
| Deploy lại sau khi sửa code/env | chạy lại lệnh `gcloud run deploy ...` ở Phần 4 |
| Xem log | `gcloud run services logs read smart-meet-be --region asia-southeast1` |
| Xem URL | `gcloud run services describe smart-meet-be --region asia-southeast1 --format="value(status.url)"` |
| Không cold start (ngày bảo vệ) | thêm `--min-instances 1` vào lệnh deploy (dùng credit, hết cold start) |

---

## ⚠️ Lưu ý riêng của Cloud Run

1. **Cold start**: `min-instances 0` → app ngủ khi rảnh, request đầu chờ JVM khởi động (~15–30s). **Ngày bảo vệ** đổi `--min-instances 1` để luôn thức (credit $300 phủ được).
2. **Mail**: Cloud Run chặn SMTP 25/465/587 giống Render → phải dùng **Brevo:2525**. Mở comment `MAIL_HOST`/`MAIL_PORT` trong `env.yaml` khi có SMTP key Brevo.
3. **$300 / 90 ngày**: đủ cho tới lúc bảo vệ. Hết credit thì app dừng (không tự trừ tiền). Traffic thấp thì có thể vẫn nằm trong free tier của Cloud Run sau đó (với `min-instances 0`).
4. `env.yaml` chứa secret → không commit (đã ignore). Giữ 1 bản ở máy để paste lại khi cần.
