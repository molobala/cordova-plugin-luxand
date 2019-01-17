//
//  based on ColorTrackingViewController.h
//  from ColorTracking application
//  The source code for this application is available under a BSD license.
//  See ColorTrackingLicense.txt for details.
//  Created by Brad Larson on 10/7/2010.
//  Modified by Anton Malyshev on 6/21/2013.
//

#import <UIKit/UIKit.h>
#import "RecognitionCamera.h"
#import "RecognitionGLView.h"
#include "LuxandFaceSDK.h"

#define MAX_FACES 5

#define MAX_NAME_LEN 1024
@class LuxandProcessor;

FOUNDATION_EXPORT int const ALREADY_REGISTERED;
FOUNDATION_EXPORT int const REGISTERED;
FOUNDATION_EXPORT int const NOT_REGISTERED ;
FOUNDATION_EXPORT int const RECOGNIZED;
FOUNDATION_EXPORT int const NOT_RECOGNIZED;

typedef struct {
    unsigned char * buffer;
    int width, height, scanline;
    float ratio;
} DetectFaceParams;

typedef struct {
    int x1, x2, y1, y2;
} FaceRectangle;


@interface RecognitionViewController : UIViewController <RecognitionCameraDelegate>
{
	RecognitionCamera * camera;
    LuxandProcessor* luxandProcessor;
	UIScreen * screenForDisplay;
    
    GLuint directDisplayProgram;
	GLuint videoFrameTexture;
	GLubyte * rawPositionPixels;

    NSLock * enteredNameLock;
    char * enteredName;
    NSString* correspondingName;
    NSString* generatedName;
    long correspondingId;
    volatile int namedFaceID;
    BOOL identifying;
    BOOL identified;
    long tryCount;
    long initialTryCount;
    long long startTime;
    CALayer * trackingRects[MAX_FACES];
    CATextLayer * nameLabels[MAX_FACES];
    
    //volatile int processingImage;
    
    NSLock * faceDataLock;
    FaceRectangle faces[MAX_FACES];
    NSLock * nameDataLock;
    char * names[MAX_FACES];
    NSString* mAttributeValues[MAX_FACES];
    long long IDs[MAX_FACES];
    volatile int faceTouched;
    volatile int indexOfTouchedFace;
    NSLock * idOfTouchedFaceLock;
    long long idOfTouchedFace;
    CGPoint currentTouchPoint;
	
    volatile int rotating;
    char videoStarted;
    
    volatile int clearTracker;
    UIToolbar * toolbar;
    
    //UIImage * image_for_screenshot;
    
    //NOTE: use locks accessing (volatile int) variables if int is not machine word 
}

@property(readonly) RecognitionGLView * glView;
@property(readonly) HTracker tracker;
@property(readwrite) NSString * templatePath;
@property(readwrite) volatile int closing;
@property(readonly) volatile int processingImage;

// Initialization and teardown
- (id)initWithProcessor:(UIScreen *)newScreenForDisplay processor:(LuxandProcessor*) processor;
-(NSString*) generateName;
-(NSString*) performRegistrationAgain: (long) id;
-(int)register: (long) id;
-(void) response: (BOOL) error message:(NSString*) message extra:(NSString*) extra;
-(bool) recognize: (long) id ;
// OpenGL ES 2.0 setup methods
- (BOOL)loadVertexShader:(NSString *)vertexShaderName fragmentShader:(NSString *)fragmentShaderName forProgram:(GLuint *)programPointer;
- (BOOL)compileShader:(GLuint *)shader type:(GLenum)type file:(NSString *)file;
- (BOOL)linkProgram:(GLuint)prog;
- (BOOL)validateProgram:(GLuint)prog;

// Device rotating support
- (void)relocateSubviewsForOrientation:(UIInterfaceOrientation)orientation;

// Image processing in FaceSDK
- (void)processImageAsyncWith:(NSData *)args;

@end

