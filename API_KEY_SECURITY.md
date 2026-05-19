# 🔐 BẢO MẬT API KEY

## ⚠️ QUAN TRỌNG: Không push API key lên Git!

File `secrets.xml` chứa API key **ĐÃ ĐƯỢC THÊM VÀO .gitignore** để không bị push lên GitHub.

## 📝 Hướng dẫn cho người khác clone project:

1. Copy file `secrets.xml.example` thành `secrets.xml`:
   ```bash
   cp app/src/main/res/values/secrets.xml.example app/src/main/res/values/secrets.xml
   ```

2. Mở `secrets.xml` và thay `YOUR_API_KEY_HERE` bằng API key của bạn

3. File `secrets.xml` sẽ KHÔNG được push lên Git (đã có trong .gitignore)

## 🔑 Lấy API key ở đâu?

Truy cập: https://aistudio.google.com/apikey

## ✅ Kiểm tra xem secrets.xml có bị push không:

```bash
git status
```

Nếu thấy `secrets.xml` trong danh sách → Chạy:
```bash
git rm --cached app/src/main/res/values/secrets.xml
git commit -m "Remove secrets.xml from tracking"
```
