const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

/**
 * FCM mesajlarını gönderen Cloud Function
 * 
 * Bu fonksiyon, Firestore'da fcm_messages koleksiyonuna eklenen dokümanları dinler
 * ve FCM mesajı gönderir. Mesaj gönderildikten sonra doküman silinir.
 */
exports.sendFCMMessage = functions.firestore
  .document('fcm_messages/{messageId}')
  .onCreate(async (snapshot, context) => {
    try {
      const messageData = snapshot.data();
      
      if (!messageData) {
        console.log('Mesaj verisi bulunamadı');
        return null;
      }
      
      // FCM mesajını hazırla
      const message = {
        token: messageData.token,
        notification: messageData.notification,
        data: messageData.data || {},
        android: {
          priority: 'high',
          notification: {
            channelId: messageData.data?.type === 'connection' ? 'connection_channel' : 
                      messageData.data?.type === 'card' ? 'card_channel' : 
                      messageData.data?.type === 'promo' ? 'promo_channel' : 
                      'default_channel'
          }
        },
        apns: {
          payload: {
            aps: {
              contentAvailable: true,
              badge: 1,
              sound: 'default'
            }
          }
        }
      };
      
      // FCM mesajını gönder
      const response = await admin.messaging().send(message);
      console.log('Mesaj başarıyla gönderildi:', response);
      
      // İşlem tamamlandıktan sonra dokümanı sil
      await snapshot.ref.delete();
      
      return null;
    } catch (error) {
      console.error('Mesaj gönderilirken hata oluştu:', error);
      return null;
    }
  });

/**
 * Yeni bağlantı isteği geldiğinde bildirim gönderen Cloud Function
 */
exports.onNewConnectionRequest = functions.firestore
  .document('users/{userId}/connectRequests/{requestId}')
  .onCreate(async (snapshot, context) => {
    try {
      const userId = context.params.userId;
      const requestData = snapshot.data();
      
      if (!requestData || !requestData.userId) {
        console.log('Bağlantı isteği verisi bulunamadı');
        return null;
      }
      
      // İstek gönderen kullanıcının bilgilerini al
      const senderDoc = await admin.firestore().collection('users').doc(requestData.userId).get();
      const senderData = senderDoc.data();
      
      if (!senderData) {
        console.log('Gönderen kullanıcı bulunamadı');
        return null;
      }
      
      // Alıcı kullanıcının bilgilerini al
      const receiverDoc = await admin.firestore().collection('users').doc(userId).get();
      const receiverData = receiverDoc.data();
      
      if (!receiverData || !receiverData.fcmToken) {
        console.log('Alıcı kullanıcı veya FCM token bulunamadı');
        return null;
      }
      
      // Bildirim içeriğini hazırla
      const senderName = senderData.displayName || 'Bir kullanıcı';
      const notificationId = admin.firestore().collection('users').doc(userId).collection('notifications').doc().id;
      
      // Bildirim verisini kaydet
      await admin.firestore().collection('users').doc(userId).collection('notifications').doc(notificationId).set({
        id: notificationId,
        type: 'connection',
        senderId: requestData.userId,
        senderName: senderName,
        receiverId: userId,
        title: 'Yeni Bağlantı İsteği',
        body: `${senderName} sizinle bağlantı kurmak istiyor`,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        read: false,
        received: false
      });
      
      // FCM mesajını gönder
      const message = {
        token: receiverData.fcmToken,
        notification: {
          title: 'Yeni Bağlantı İsteği',
          body: `${senderName} sizinle bağlantı kurmak istiyor`
        },
        data: {
          type: 'connection',
          senderId: requestData.userId,
          notificationId: notificationId
        },
        android: {
          priority: 'high',
          notification: {
            channelId: 'connection_channel'
          }
        },
        apns: {
          payload: {
            aps: {
              contentAvailable: true,
              badge: 1,
              sound: 'default'
            }
          }
        }
      };
      
      await admin.messaging().send(message);
      console.log('Bağlantı isteği bildirimi gönderildi');
      
      return null;
    } catch (error) {
      console.error('Bağlantı isteği bildirimi gönderilirken hata oluştu:', error);
      return null;
    }
  });

/**
 * Kart görüntüleme istatistiklerini güncelleyen Cloud Function
 */
exports.updateCardViewStatistics = functions.firestore
  .document('cards/{cardId}/views/{viewId}')
  .onCreate(async (snapshot, context) => {
    try {
      const cardId = context.params.cardId;
      const viewData = snapshot.data();
      
      if (!viewData || !viewData.userId) {
        console.log('Görüntüleme verisi bulunamadı');
        return null;
      }
      
      // Kart dokümanını al
      const cardRef = admin.firestore().collection('cards').doc(cardId);
      const cardDoc = await cardRef.get();
      const cardData = cardDoc.data();
      
      if (!cardData) {
        console.log('Kart bulunamadı');
        return null;
      }
      
      // Görüntüleme sayısını arttır
      await cardRef.update({
        viewCount: admin.firestore.FieldValue.increment(1),
        lastViewedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      
      // Kart sahibi ve görüntüleyen aynı kişi değilse bildirim gönder
      if (cardData.userId !== viewData.userId) {
        // Görüntüleyen kullanıcının bilgilerini al
        const viewerDoc = await admin.firestore().collection('users').doc(viewData.userId).get();
        const viewerData = viewerDoc.data();
        
        if (!viewerData) {
          console.log('Görüntüleyen kullanıcı bulunamadı');
          return null;
        }
        
        // Kart sahibinin bilgilerini al
        const ownerDoc = await admin.firestore().collection('users').doc(cardData.userId).get();
        const ownerData = ownerDoc.data();
        
        if (!ownerData || !ownerData.fcmToken) {
          console.log('Kart sahibi veya FCM token bulunamadı');
          return null;
        }
        
        // Bildirim içeriğini hazırla
        const viewerName = viewerData.displayName || 'Bir kullanıcı';
        const cardName = cardData.name || 'Kartvizitiniz';
        const notificationId = admin.firestore().collection('users').doc(cardData.userId).collection('notifications').doc().id;
        
        // Bildirim verisini kaydet
        await admin.firestore().collection('users').doc(cardData.userId).collection('notifications').doc(notificationId).set({
          id: notificationId,
          type: 'card',
          senderId: viewData.userId,
          senderName: viewerName,
          receiverId: cardData.userId,
          cardId: cardId,
          cardName: cardName,
          title: 'Kartınız Görüntülendi',
          body: `${viewerName}, ${cardName} kartınızı görüntüledi`,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          read: false,
          received: false
        });
        
        // FCM mesajını gönder
        const message = {
          token: ownerData.fcmToken,
          notification: {
            title: 'Kartınız Görüntülendi',
            body: `${viewerName}, ${cardName} kartınızı görüntüledi`
          },
          data: {
            type: 'card',
            cardId: cardId,
            senderId: viewData.userId,
            notificationId: notificationId
          },
          android: {
            priority: 'normal',
            notification: {
              channelId: 'card_channel'
            }
          },
          apns: {
            payload: {
              aps: {
                contentAvailable: true,
                badge: 1,
                sound: 'default'
              }
            }
          }
        };
        
        await admin.messaging().send(message);
        console.log('Kart görüntüleme bildirimi gönderildi');
      }
      
      return null;
    } catch (error) {
      console.error('Kart görüntüleme istatistikleri güncellenirken hata oluştu:', error);
      return null;
    }
  });

/**
 * Bağlantı isteği kabul edildiğinde bildirim gönderen Cloud Function
 */
exports.onConnectionAccepted = functions.firestore
  .document('users/{userId}/connections/{connectionId}')
  .onCreate(async (snapshot, context) => {
    try {
      const userId = context.params.userId;
      const connectionData = snapshot.data();
      
      if (!connectionData || !connectionData.userId) {
        console.log('Bağlantı verisi bulunamadı');
        return null;
      }
      
      // Bağlantı isteğini kabul eden kullanıcının bilgilerini al
      const accepterDoc = await admin.firestore().collection('users').doc(userId).get();
      const accepterData = accepterDoc.data();
      
      if (!accepterData) {
        console.log('Kabul eden kullanıcı bulunamadı');
        return null;
      }
      
      // İstek gönderen kullanıcının bilgilerini al
      const senderDoc = await admin.firestore().collection('users').doc(connectionData.userId).get();
      const senderData = senderDoc.data();
      
      if (!senderData || !senderData.fcmToken) {
        console.log('Gönderen kullanıcı veya FCM token bulunamadı');
        return null;
      }
      
      // Bildirim içeriğini hazırla
      const accepterName = accepterData.displayName || 'Bir kullanıcı';
      const notificationId = admin.firestore().collection('users').doc(connectionData.userId).collection('notifications').doc().id;
      
      // Bildirim verisini kaydet
      await admin.firestore().collection('users').doc(connectionData.userId).collection('notifications').doc(notificationId).set({
        id: notificationId,
        type: 'connection_accepted',
        senderId: userId,
        senderName: accepterName,
        receiverId: connectionData.userId,
        title: 'Bağlantı İsteği Kabul Edildi',
        body: `${accepterName} bağlantı isteğinizi kabul etti`,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        read: false,
        received: false
      });
      
      // FCM mesajını gönder
      const message = {
        token: senderData.fcmToken,
        notification: {
          title: 'Bağlantı İsteği Kabul Edildi',
          body: `${accepterName} bağlantı isteğinizi kabul etti`
        },
        data: {
          type: 'connection_accepted',
          senderId: userId,
          notificationId: notificationId
        },
        android: {
          priority: 'high',
          notification: {
            channelId: 'connection_channel'
          }
        },
        apns: {
          payload: {
            aps: {
              contentAvailable: true,
              badge: 1,
              sound: 'default'
            }
          }
        }
      };
      
      await admin.messaging().send(message);
      console.log('Bağlantı kabul bildirimi gönderildi');
      
      return null;
    } catch (error) {
      console.error('Bağlantı kabul bildirimi gönderilirken hata oluştu:', error);
      return null;
    }
  });

/**
 * Okunmamış bildirim sayısını güncelleyen Cloud Function
 */
exports.updateUnreadNotificationCount = functions.firestore
  .document('users/{userId}/notifications/{notificationId}')
  .onWrite(async (change, context) => {
    try {
      const userId = context.params.userId;
      
      // Okunmamış bildirimleri say
      const snapshot = await admin.firestore()
        .collection('users')
        .doc(userId)
        .collection('notifications')
        .where('read', '==', false)
        .get();
      
      const unreadCount = snapshot.size;
      
      // Kullanıcı dokümanını güncelle
      await admin.firestore()
        .collection('users')
        .doc(userId)
        .update({
          unreadNotificationCount: unreadCount
        });
      
      return null;
    } catch (error) {
      console.error('Okunmamış bildirim sayısı güncellenirken hata oluştu:', error);
      return null;
    }
  });
