#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>

@class RecognitionViewController;


@interface Luxand : CDVPlugin {}
    -(void)init: (CDVInvokedUrlCommand*)command;
    -(void)login: (CDVInvokedUrlCommand*)command;
    -(void)register: (CDVInvokedUrlCommand*)command;
    -(void)clear: (CDVInvokedUrlCommand*)command;
    -(void)clearMemory: (CDVInvokedUrlCommand*)command;

    -(void)sendSuccess:(NSDictionary*)data commandId: (NSString*) callbackId;
    -(void)sendError:(NSDictionary*)data commandId: (NSString*) callbackId;
    -(BOOL)isUsageDescriptionSet;
    -(BOOL)notHasPermission;
    @property (nonatomic, readwrite) long tryCount;
    @property (nonatomic, readwrite) NSString* licence;
    @property (nonatomic, readwrite) NSString* dbName;
    @property (nonatomic, readwrite) char * templatePath;
    @property (nonatomic, readwrite) LuxandProcessor* processor;
@end


@interface LuxandProcessor : NSObject {
    RecognitionViewController*    viewController;
}
@property (nonatomic, retain) Luxand*           plugin;
@property (nonatomic, retain) NSString*                   callback;
@property (nonatomic, retain) UIViewController*  parentViewController;
@property (nonatomic, readwrite) long timeout;
@property (nonatomic, readwrite) NSString* licence;
@property (nonatomic, readwrite) long tryCount;
@property (nonatomic, readwrite) BOOL identifying;
@property(nonatomic, retain) NSString* templatePath;
- (id)initWithPlugin:(Luxand*)plugin callback:(NSString*)callback parentViewController: (UIViewController*) parentViewController licence : (NSString*) licence timeout: (long) timeout retryCount: (long) retry identifying:(BOOL) forIdenftifying templatePath: (NSString*) dbPath;
- (void) login;
- (void) register;
-(void) sendResult: (NSDictionary*) data;
@end
