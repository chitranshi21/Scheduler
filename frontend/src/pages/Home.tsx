import { useNavigate } from 'react-router-dom';
import { useUser } from '@clerk/clerk-react';
import { useEffect } from 'react';
import './Home.css';

// Helper function to get redirect URL based on role
const getRedirectUrl = (role: string): string => {
  if (role === 'ADMIN') {
    return '/admin';
  } else if (role === 'BUSINESS') {
    return '/business';
  }
  return '/'; // CUSTOMER or no role - stay on home page
};

export default function Home() {
  const navigate = useNavigate();
  const { isSignedIn, user, isLoaded } = useUser();

  // Redirect logged-in users to their dashboard
  useEffect(() => {
    if (isLoaded && isSignedIn && user) {
      const userRole = user.publicMetadata?.role as string || 'CUSTOMER';
      const redirectUrl = getRedirectUrl(userRole);
      
      // Only redirect if not already on the correct page
      if (redirectUrl !== '/') {
        navigate(redirectUrl, { replace: true });
      }
    }
  }, [isSignedIn, user, isLoaded, navigate]);

  return (
    <div className="home-page">
      {/* Navigation Bar */}
      <nav className="home-nav">
        <div className="nav-container">
          <div className="nav-logo">
            <span className="logo-icon">📅</span>
            <span className="logo-text">SessionScheduler</span>
          </div>
          <div className="nav-links">
            <a href="#features" className="nav-link">Features</a>
            <a href="#how-it-works" className="nav-link">How It Works</a>
            <a href="#pricing" className="nav-link">Pricing</a>
            <button 
              className="nav-login-btn"
              onClick={() => navigate('/login')}
            >
              Login
            </button>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-container">
          <div className="hero-content">
            <h1 className="hero-title">
              Book Your Perfect
              <span className="gradient-text"> Online Class</span>
            </h1>
            <p className="hero-subtitle">
              Discover and book from a curated selection of online classes. 
              From yoga to business coaching, find the perfect session that fits your schedule.
            </p>
            <div className="hero-cta">
              <button 
                className="btn-primary"
                onClick={() => navigate('/book/demo-yoga')}
              >
                Browse Classes
              </button>
              <button 
                className="btn-secondary"
                onClick={() => navigate('/login')}
              >
                Get Started
              </button>
            </div>
            <div className="hero-stats">
              <div className="stat-item">
                <div className="stat-number">1000+</div>
                <div className="stat-label">Active Classes</div>
              </div>
              <div className="stat-item">
                <div className="stat-number">50+</div>
                <div className="stat-label">Expert Instructors</div>
              </div>
              <div className="stat-item">
                <div className="stat-number">24/7</div>
                <div className="stat-label">Available Booking</div>
              </div>
            </div>
          </div>
          <div className="hero-image">
            <div className="hero-card">
              <div className="card-header">
                <div className="card-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
              <div className="card-content">
                <div className="calendar-preview">
                  <div className="calendar-day selected">13</div>
                  <div className="calendar-day">14</div>
                  <div className="calendar-day">15</div>
                </div>
                <div className="time-slots-preview">
                  <div className="time-slot-preview">9:00 AM</div>
                  <div className="time-slot-preview active">10:30 AM</div>
                  <div className="time-slot-preview">12:00 PM</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="features-section">
        <div className="section-container">
          <div className="section-header">
            <h2 className="section-title">Why Choose SessionScheduler?</h2>
            <p className="section-subtitle">
              Everything you need to discover and book online classes seamlessly
            </p>
          </div>
          <div className="features-grid">
            <div className="feature-card">
              <div className="feature-icon">🎯</div>
              <h3 className="feature-title">Curated Sessions</h3>
              <p className="feature-description">
                Browse through carefully selected online classes from verified instructors. 
                Each session is designed to deliver value and meet your learning goals.
              </p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">⏰</div>
              <h3 className="feature-title">Flexible Scheduling</h3>
              <p className="feature-description">
                Book classes that fit your schedule. View real-time availability, 
                choose your preferred time slot, and get instant confirmation.
              </p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">💳</div>
              <h3 className="feature-title">Secure Payments</h3>
              <p className="feature-description">
                Safe and secure payment processing. Pay for your classes with confidence 
                using our integrated payment system.
              </p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">📧</div>
              <h3 className="feature-title">Instant Confirmations</h3>
              <p className="feature-description">
                Receive immediate booking confirmations via email with calendar invites. 
                Never miss a class with automatic reminders.
              </p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">🌐</div>
              <h3 className="feature-title">Virtual Meetings</h3>
              <p className="feature-description">
                Join classes from anywhere. Integrated video meeting links make it easy 
                to connect with instructors and fellow learners.
              </p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">📱</div>
              <h3 className="feature-title">Mobile Friendly</h3>
              <p className="feature-description">
                Book on the go. Our responsive design works perfectly on any device, 
                making it easy to schedule classes from your phone or tablet.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section id="how-it-works" className="how-it-works-section">
        <div className="section-container">
          <div className="section-header">
            <h2 className="section-title">How It Works</h2>
            <p className="section-subtitle">
              Book your first class in just three simple steps
            </p>
          </div>
          <div className="steps-container">
            <div className="step-item">
              <div className="step-number">1</div>
              <div className="step-content">
                <h3 className="step-title">Browse Available Classes</h3>
                <p className="step-description">
                  Explore our catalog of online classes. Filter by category, duration, 
                  or instructor to find exactly what you're looking for.
                </p>
              </div>
            </div>
            <div className="step-item">
              <div className="step-number">2</div>
              <div className="step-content">
                <h3 className="step-title">Select Your Time</h3>
                <p className="step-description">
                  Choose a date and time that works for you. Our calendar shows real-time 
                  availability, so you can see what's open right now.
                </p>
              </div>
            </div>
            <div className="step-item">
              <div className="step-number">3</div>
              <div className="step-content">
                <h3 className="step-title">Complete Your Booking</h3>
                <p className="step-description">
                  Enter your details, confirm payment, and you're all set! You'll receive 
                  a confirmation email with all the details and meeting links.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="cta-section">
        <div className="section-container">
          <div className="cta-content">
            <h2 className="cta-title">Ready to Start Learning?</h2>
            <p className="cta-subtitle">
              Join thousands of learners who are already booking classes and advancing their skills.
            </p>
            <div className="cta-buttons">
              <button 
                className="btn-primary btn-large"
                onClick={() => navigate('/book/demo-yoga')}
              >
                Browse Classes Now
              </button>
              <button 
                className="btn-secondary btn-large"
                onClick={() => navigate('/login')}
              >
                Create Account
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="home-footer">
        <div className="footer-container">
          <div className="footer-content">
            <div className="footer-section">
              <div className="footer-logo">
                <span className="logo-icon">📅</span>
                <span className="logo-text">SessionScheduler</span>
              </div>
              <p className="footer-description">
                The easiest way to discover and book online classes from expert instructors.
              </p>
            </div>
            <div className="footer-section">
              <h4 className="footer-title">Product</h4>
              <ul className="footer-links">
                <li><a href="#features">Features</a></li>
                <li><a href="#how-it-works">How It Works</a></li>
                <li><a href="#pricing">Pricing</a></li>
              </ul>
            </div>
            <div className="footer-section">
              <h4 className="footer-title">Company</h4>
              <ul className="footer-links">
                <li><a href="#">About</a></li>
                <li><a href="#">Blog</a></li>
                <li><a href="#">Contact</a></li>
              </ul>
            </div>
            <div className="footer-section">
              <h4 className="footer-title">Support</h4>
              <ul className="footer-links">
                <li><a href="#">Help Center</a></li>
                <li><a href="#">Privacy Policy</a></li>
                <li><a href="#">Terms of Service</a></li>
              </ul>
            </div>
          </div>
          <div className="footer-bottom">
            <p>&copy; 2024 SessionScheduler. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}

