//
//  Luxand.m
//  Luxand Face Recognition
//
//  Created by Mahamadou DOUMBIA on 15/01/2019.
//

// #import <AVFoundation/AVFoundation.h>
#import <Cordova/CDVPlugin.h>
#include "LuxandFaceSDK.h"
#include "RecognitionViewController.h"
#include "Luxand.h"


@implementation Luxand


@synthesize licence = _licence;
@synthesize dbName = _dbName;
@synthesize tryCount = _tryCount;
@synthesize templatePath = _templatePath;

//@synthesize processor = _processor;
-(BOOL)notHasPermission
{
    AVAuthorizationStatus authStatus = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    return (authStatus == AVAuthorizationStatusDenied ||
            authStatus == AVAuthorizationStatusRestricted);
}

-(BOOL)isUsageDescriptionSet
{
    NSDictionary * plist = [[NSBundle mainBundle] infoDictionary];
    if ([plist objectForKey:@"NSCameraUsageDescription" ] ||
        [[NSBundle mainBundle] localizedStringForKey: @"NSCameraUsageDescription" value: nil table: @"InfoPlist"]) {
        return YES;
    }
    return NO;
}

- (void)init: (CDVInvokedUrlCommand*)command;
{
    _licence = [command.arguments objectAtIndex:0];
    _dbName = [command.arguments objectAtIndex:1];
    _tryCount = [command.arguments[2] integerValue];
    NSString * templatePathO = [NSHomeDirectory() stringByAppendingPathComponent:[@"Documents/" stringByAppendingString: _dbName]];
    const char * templatePath = [templatePathO UTF8String];
    _templatePath = (char *)malloc(strlen(templatePath)+1);
    strcpy(_templatePath, templatePath);
    NSLog(@"Plugin args %s %s %ld\n", [_licence UTF8String], _templatePath, _tryCount);
    int res = FSDKE_OK;
    res = FSDK_ActivateLibrary([_licence UTF8String]);
    #if defined(DEBUG)
        NSLog(@"activation result %d\n", res);
    #endif
    if (res) {
        CDVPluginResult* result = [CDVPluginResult
                                       resultWithStatus: CDVCommandStatus_ERROR
                                       messageAsString: @"Echeck d'initialization"
                                   ];
        [self.commandDelegate sendPluginResult:result callbackId: command.callbackId];
    }
    res = FSDK_Initialize((char *)"");
    #if defined(DEBUG)
        NSLog(@"init result %d\n", res);
    #endif
    if (res) {
        CDVPluginResult* result = [CDVPluginResult
                                   resultWithStatus: CDVCommandStatus_ERROR
                                   messageAsString: @"Echeck d'initialization"
                                   ];
        [self.commandDelegate sendPluginResult:result callbackId: command.callbackId];
    }
    CDVPluginResult* result = [CDVPluginResult
                               resultWithStatus: CDVCommandStatus_OK
                               messageAsString: @"FSDK initialized successfully"
                               ];
    [self.commandDelegate sendPluginResult:result callbackId: command.callbackId];
}

- (void)register: (CDVInvokedUrlCommand*)command;
{
    long timeout = [command.arguments[0] integerValue];
    _processor = [[LuxandProcessor alloc] initWithPlugin:self callback: command.callbackId parentViewController: self.viewController licence: self.licence timeout: timeout retryCount: self.tryCount identifying: true templatePath: [NSString stringWithUTF8String: self.templatePath]];
    //laucn with
    [_processor performSelector:@selector(register) withObject:nil afterDelay:0];
}
- (void)login: (CDVInvokedUrlCommand*)command;
{
    long timeout = [command.arguments[0] integerValue];
    _processor = [[LuxandProcessor alloc] initWithPlugin:self callback: command.callbackId parentViewController: self.viewController licence: self.licence timeout: timeout retryCount: self.tryCount identifying: false templatePath: [NSString stringWithUTF8String: self.templatePath]];
    //laucn with
    [_processor performSelector:@selector(login) withObject:nil afterDelay:0];
}
-(void) clear:(CDVInvokedUrlCommand *)command {
    NSDictionary* ret = [[NSDictionary alloc] initWithObjectsAndKeys:@"FAIL", @"status", nil];
    [self sendError:ret commandId:command.callbackId];
    return;
}
-(void) clearMemory:(CDVInvokedUrlCommand *)command {
    HTracker tracker;
    if(FSDKE_OK != FSDK_LoadTrackerMemoryFromFile(&tracker, _templatePath))
    FSDK_CreateTracker(&tracker);
    if(FSDKE_OK != FSDK_ClearTracker(tracker)) {
        NSDictionary* ret = [[NSDictionary alloc] initWithObjectsAndKeys:@"FAIL", @"status", nil];
         //NSLog(@"ERROR cleared");
        [self sendError:ret commandId:command.callbackId];
        return;
    }
    NSDictionary* ret = [[NSDictionary alloc] initWithObjectsAndKeys:@"SUCCESS", @"status", nil];
    FSDK_SaveTrackerMemoryToFile(tracker, _templatePath);
    //NSLog(@"SUCESS cleared");
    [self sendError:ret commandId:command.callbackId];
}

-(void) sendSuccess:(NSDictionary*)data commandId:(NSString*) callbackId {
    //NSLog(@"%@", data);
    CDVPluginResult* result = [CDVPluginResult
                               resultWithStatus: CDVCommandStatus_OK
                               messageAsDictionary: data
                               ];
    [self.commandDelegate sendPluginResult:result callbackId: callbackId];
}
-(void) sendError:(NSDictionary*)data commandId:(NSString*) callbackId {
    CDVPluginResult* result = [CDVPluginResult
                               resultWithStatus: CDVCommandStatus_OK
                               messageAsDictionary: data
                               ];
    [self.commandDelegate sendPluginResult:result callbackId: callbackId];
}

@end

@implementation LuxandProcessor

- (id)initWithPlugin:(Luxand*)plugin callback:(NSString*)callback parentViewController: (UIViewController*) parentViewController licence : (NSString*) licence timeout: (long) timeout retryCount: (long) retry identifying:(BOOL) forIdenftifying templatePath: (NSString*) dbPath
{
    self.templatePath = dbPath;
    self.plugin = plugin;
    self.licence = licence;
    self.tryCount = retry;
    self.identifying = forIdenftifying;
    self.timeout = timeout;
    self.callback = callback;
    self.parentViewController = parentViewController;
    return self;
}
- (void)dealloc {
    self.plugin = nil;
    self.callback = nil;
    self.parentViewController = nil;
    self.parentViewController = nil;
}
- (void) register{
    viewController = [[RecognitionViewController alloc] initWithProcessor: [UIScreen mainScreen] processor:self];
    [self.parentViewController presentViewController: viewController animated: false completion:nil];
}
- (void) login{
    viewController = [[RecognitionViewController alloc] initWithProcessor: [UIScreen mainScreen] processor:self];
    [self.parentViewController presentViewController: viewController animated: false completion:nil];
}
-(void) sendResult: (NSDictionary*) data {
    [self.plugin sendSuccess:data commandId: self.callback];
    //quit app
    [viewController dismissViewControllerAnimated:YES completion:Nil];
}
@end
