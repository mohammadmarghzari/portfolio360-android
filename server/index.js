/**
 * ربات تأیید خودکار اشتراک Portfolio360.
 *
 * جریان کار:
 *   ۱. کاربر از داخل اپ روی «ارسال رسید» می‌زند → ربات با پارامتر start=<uid> باز
 *      می‌شود و ربات، چت او را به حساب کاربری‌اش گره می‌زند.
 *   ۲. کاربر عکس رسید را برای ربات می‌فرستد → ربات آن را با دکمه‌های تأیید پلن
 *      به چت مدیر فوروارد می‌کند.
 *   ۳. مدیر روی پلن موردنظر می‌زند → ربات با Admin SDK سند اشتراک را در Firestore
 *      می‌نویسد و اشتراک همان کاربر بلافاصله فعال می‌شود.
 *
 * همه‌ی مقادیر حساس از متغیرهای محیطی (Environment Variables) خوانده می‌شوند و
 * هیچ کلیدی داخل کد نیست:
 *   BOT_TOKEN                 توکن ربات تلگرام (از BotFather)
 *   ADMIN_CHAT_ID             آیدی عددی چت مدیر (از @userinfobot)
 *   FIREBASE_SERVICE_ACCOUNT  کل محتوای فایل service-account به‌صورت JSON یک‌خطی
 */
const express = require("express");
const TelegramBot = require("node-telegram-bot-api");
const admin = require("firebase-admin");

const BOT_TOKEN = process.env.BOT_TOKEN;
const ADMIN_CHAT_ID = process.env.ADMIN_CHAT_ID;

if (!BOT_TOKEN || !ADMIN_CHAT_ID || !process.env.FIREBASE_SERVICE_ACCOUNT) {
  console.error("متغیرهای محیطی BOT_TOKEN / ADMIN_CHAT_ID / FIREBASE_SERVICE_ACCOUNT تنظیم نشده‌اند.");
  process.exit(1);
}

const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

const PLAN_DAYS = { "1m": 30, "3m": 90, "6m": 180, "1y": 365 };
const PLAN_LABELS = { "1m": "۱ ماهه", "3m": "۳ ماهه", "6m": "۶ ماهه", "1y": "۱ ساله" };

const bot = new TelegramBot(BOT_TOKEN, { polling: true });

// ۱) اتصال چت کاربر به حساب او
bot.onText(/\/start(?:\s+(.+))?/, async (msg, match) => {
  const chatId = msg.chat.id;
  const uid = match && match[1] ? match[1].trim() : null;
  try {
    if (uid) {
      await db.collection("telegram_links").doc(String(chatId)).set({ uid, linkedAt: Date.now() });
      await bot.sendMessage(chatId, "حساب شما وصل شد ✅\nحالا عکس رسید واریزی رو همینجا بفرست.");
    } else {
      await bot.sendMessage(chatId, "برای ارسال رسید، لطفاً از داخل اپ Portfolio360 دکمه «ارسال رسید» رو بزن.");
    }
  } catch (e) {
    console.error("start error:", e.message);
  }
});

// ۲) دریافت عکس رسید از کاربر و فوروارد به مدیر با دکمه‌های تأیید
bot.on("photo", async (msg) => {
  const chatId = msg.chat.id;
  if (String(chatId) === String(ADMIN_CHAT_ID)) return;

  try {
    const linkDoc = await db.collection("telegram_links").doc(String(chatId)).get();
    const uid = linkDoc.exists ? linkDoc.data().uid : null;
    const fileId = msg.photo[msg.photo.length - 1].file_id;
    const sender = msg.from.username ? "@" + msg.from.username : msg.from.first_name;

    const caption = uid
      ? `📩 رسید جدید\nکاربر (uid): ${uid}\nفرستنده: ${sender}`
      : `⚠️ رسید بدون شناسه — کاربر از لینک اپ وارد نشده\nفرستنده: ${sender} (چت ${chatId})`;

    const replyMarkup = uid
      ? {
          inline_keyboard: [
            [
              { text: "۱ ماهه", callback_data: `approve:${uid}:${chatId}:1m` },
              { text: "۳ ماهه", callback_data: `approve:${uid}:${chatId}:3m` },
            ],
            [
              { text: "۶ ماهه", callback_data: `approve:${uid}:${chatId}:6m` },
              { text: "۱ ساله", callback_data: `approve:${uid}:${chatId}:1y` },
            ],
          ],
        }
      : undefined;

    await bot.sendPhoto(ADMIN_CHAT_ID, fileId, { caption, reply_markup: replyMarkup });
    await bot.sendMessage(chatId, "رسید دریافت شد ✅ بعد از بررسی، اشتراکت فعال می‌شه.");
  } catch (e) {
    console.error("photo error:", e.message);
  }
});

// ۳) تأیید مدیر → نوشتن اشتراک در Firestore
bot.on("callback_query", async (cb) => {
  try {
    if (String(cb.message.chat.id) !== String(ADMIN_CHAT_ID)) return;
    const [action, uid, userChatId, planKey] = (cb.data || "").split(":");
    if (action !== "approve" || !PLAN_DAYS[planKey]) return;

    const expiresAt = Date.now() + PLAN_DAYS[planKey] * 24 * 60 * 60 * 1000;
    await db.collection("subscriptions").doc(uid).set({
      active: true,
      expiresAt,
      plan: planKey,
      activatedAt: Date.now(),
    });

    await bot.answerCallbackQuery(cb.id, { text: "فعال شد ✅" });
    await bot.editMessageCaption(`${cb.message.caption || ""}\n\n✅ اشتراک ${PLAN_LABELS[planKey]} فعال شد.`, {
      chat_id: ADMIN_CHAT_ID,
      message_id: cb.message.message_id,
    });
    await bot.sendMessage(userChatId, `🎉 اشتراک ${PLAN_LABELS[planKey]} شما فعال شد! برای اعمال، اپ رو ببند و دوباره باز کن.`);
  } catch (e) {
    console.error("callback error:", e.message);
  }
});

bot.on("polling_error", (e) => console.error("polling_error:", e.message));

// یک وب‌سرور کوچک تا لیارا اپ را «سالم» تشخیص دهد
const app = express();
app.get("/", (_req, res) => res.send("Portfolio360 subscription bot is running."));
app.listen(process.env.PORT || 3000, () => console.log("Health server up. Bot polling started."));
