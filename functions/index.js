/**
 * رله‌ی تلگرام برای تأیید اشتراک Portfolio360.
 *
 * وقتی کاربری از داخل اپ روی «ارسال رسید در تلگرام» بزند، به این ربات با
 * پارامتر /start=<uid> وصل می‌شود. سپس هر عکسی که در همان چت بفرستد، این
 * تابع آن را همراه با دکمه‌های ۱/۳/۶/۱۲ ماهه به چت خودِ مدیر (ADMIN_CHAT_ID)
 * فوروارد می‌کند. با لمس یکی از دکمه‌ها توسط مدیر، سند اشتراک آن کاربر در
 * Firestore (subscriptions/{uid}) به‌روزرسانی و به کاربر پیام تأیید ارسال
 * می‌شود.
 *
 * Webhook تلگرام باید به آدرس اجرای این تابع تنظیم شود (یک‌بار، از طریق
 * setWebhook)؛ راهنمای کامل استقرار در README همین پوشه است.
 */

const {onRequest} = require("firebase-functions/v2/https");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {defineSecret} = require("firebase-functions/params");

initializeApp();
const db = getFirestore();

const TELEGRAM_BOT_TOKEN = defineSecret("TELEGRAM_BOT_TOKEN");
const ADMIN_CHAT_ID = defineSecret("ADMIN_CHAT_ID");

const PLAN_DAYS = {"1m": 30, "3m": 90, "6m": 180, "1y": 365};
const PLAN_LABELS = {"1m": "۱ ماهه", "3m": "۳ ماهه", "6m": "۶ ماهه", "1y": "۱ ساله"};

exports.telegramWebhook = onRequest(
    {secrets: [TELEGRAM_BOT_TOKEN, ADMIN_CHAT_ID], region: "us-central1"},
    async (req, res) => {
      const botToken = TELEGRAM_BOT_TOKEN.value();
      const adminChatId = ADMIN_CHAT_ID.value();
      const update = req.body;

      try {
        if (update.message) {
          await handleMessage(update.message, botToken, adminChatId);
        } else if (update.callback_query) {
          await handleCallback(update.callback_query, botToken, adminChatId);
        }
      } catch (err) {
        console.error("telegramWebhook error:", err);
      }

      // تلگرام فقط منتظر ۲۰۰ سریع است؛ جزئیات پردازش اهمیتی برایش ندارد.
      res.status(200).send("ok");
    },
);

async function handleMessage(message, botToken, adminChatId) {
  const chatId = message.chat.id;
  const text = message.text || "";

  if (text.startsWith("/start")) {
    const uid = text.split(" ")[1] || null;
    if (uid) {
      await db.collection("telegram_links").doc(String(chatId)).set({
        uid, linkedAt: Date.now(),
      });
      await tgCall(botToken, "sendMessage", {
        chat_id: chatId,
        text: "حساب شما وصل شد ✅\nحالا عکس رسید واریزی رو همینجا بفرست.",
      });
    } else {
      await tgCall(botToken, "sendMessage", {
        chat_id: chatId,
        text: "برای ارسال رسید، لطفاً از داخل اپ Portfolio360 دکمه «ارسال رسید در تلگرام» رو بزن.",
      });
    }
    return;
  }

  if (message.photo && String(chatId) !== String(adminChatId)) {
    await forwardReceiptToAdmin(message, chatId, botToken, adminChatId);
  }
}

async function forwardReceiptToAdmin(message, chatId, botToken, adminChatId) {
  const linkDoc = await db.collection("telegram_links").doc(String(chatId)).get();
  const uid = linkDoc.exists ? linkDoc.data().uid : null;
  const fileId = message.photo[message.photo.length - 1].file_id;
  const senderLabel = message.from.username ? "@" + message.from.username : message.from.first_name;

  const caption = uid ?
    `📩 رسید جدید\nکاربر: ${uid}\nفرستنده: ${senderLabel} (چت ${chatId})` :
    `⚠️ رسید بدون شناسه کاربری — کاربر از لینک اپ وارد نشده\nفرستنده: ${senderLabel} (چت ${chatId})`;

  const replyMarkup = uid ? {
    inline_keyboard: [
      [
        {text: "۱ ماهه", callback_data: `approve:${uid}:${chatId}:1m`},
        {text: "۳ ماهه", callback_data: `approve:${uid}:${chatId}:3m`},
      ],
      [
        {text: "۶ ماهه", callback_data: `approve:${uid}:${chatId}:6m`},
        {text: "۱ ساله", callback_data: `approve:${uid}:${chatId}:1y`},
      ],
    ],
  } : undefined;

  await tgCall(botToken, "sendPhoto", {
    chat_id: adminChatId,
    photo: fileId,
    caption,
    reply_markup: replyMarkup,
  });

  await tgCall(botToken, "sendMessage", {
    chat_id: chatId,
    text: "رسید دریافت شد ✅ بعد از بررسی، اشتراکت فعال می‌شه.",
  });
}

async function handleCallback(callbackQuery, botToken, adminChatId) {
  // فقط خود مدیر اجازه دارد اشتراک فعال کند.
  if (String(callbackQuery.message.chat.id) !== String(adminChatId)) return;

  const [action, uid, userChatId, planKey] = (callbackQuery.data || "").split(":");
  if (action !== "approve" || !PLAN_DAYS[planKey]) return;

  const days = PLAN_DAYS[planKey];
  const expiresAt = Date.now() + days * 24 * 60 * 60 * 1000;

  await db.collection("subscriptions").doc(uid).set({
    active: true,
    expiresAt,
    plan: planKey,
    activatedAt: Date.now(),
  });

  await tgCall(botToken, "answerCallbackQuery", {
    callback_query_id: callbackQuery.id,
    text: "فعال شد ✅",
  });

  const originalCaption = callbackQuery.message.caption || "";
  await tgCall(botToken, "editMessageCaption", {
    chat_id: adminChatId,
    message_id: callbackQuery.message.message_id,
    caption: `${originalCaption}\n\n✅ اشتراک ${PLAN_LABELS[planKey]} فعال شد.`,
  });

  await tgCall(botToken, "sendMessage", {
    chat_id: userChatId,
    text: `🎉 اشتراک ${PLAN_LABELS[planKey]} شما فعال شد! برای استفاده، اپ رو ببند و دوباره باز کن.`,
  });
}

async function tgCall(botToken, method, params) {
  const response = await fetch(`https://api.telegram.org/bot${botToken}/${method}`, {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify(params),
  });
  const body = await response.json();
  if (!body.ok) {
    console.error(`Telegram API ${method} failed:`, body);
  }
  return body;
}
