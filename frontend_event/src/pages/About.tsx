

import { motion } from "framer-motion";

const eventGallery = [
  {
    src: "https://images.unsplash.com/photo-1503428593586-e225b39bddfe?auto=format&fit=crop&w=600&q=80",
    title: "Tech Innovators Summit 2025",
    description: "A premier global summit uniting technology leaders, innovators, and visionaries. Over 2,000 attendees and 50+ world-class speakers share insights on the future of technology, digital transformation, and innovation."
  },
  {
    src: "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?auto=format&fit=crop&w=600&q=80",
    title: "Startup Expo Dhaka",
    description: "Bangladesh’s largest startup exhibition, connecting founders, investors, and industry experts. The event features live pitches, networking sessions, and showcases the region’s most promising startups."
  },
  {
    src: "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?auto=format&fit=crop&w=600&q=80",
    title: "Health & Wellness Fair",
    description: "A vibrant community event dedicated to promoting healthy living. Attendees enjoy expert-led workshops, fitness demonstrations, wellness consultations, and a marketplace of local health brands."
  },
  {
    src: "https://images.unsplash.com/photo-1465101046530-73398c7f28ca?auto=format&fit=crop&w=600&q=80",
    title: "EduLeaders Conference",
    description: "An annual conference bringing together educators, students, and policymakers. The event focuses on innovative teaching strategies, education technology, and shaping the future of learning."
  },
];

export default function About() {
  return (
    <div className="min-h-screen bg-white text-gray-900">
      {/* Company Overview Section */}
      <motion.section
        initial={{ opacity: 0, y: 40 }}
        whileInView={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, type: "spring" }}
        viewport={{ once: true }}
        className="max-w-6xl mx-auto py-16 px-2 sm:px-4"
      >
        <h1 className="text-4xl font-extrabold mb-4 text-coral tracking-tight">About EventFlow</h1>
        <p className="text-[19px] text-gray-800 mb-8 leading-relaxed max-w-5xl">
          EventFlow is a leading event management platform dedicated to transforming the way people create, discover, and experience events. Our platform empowers organizers with robust tools for planning, promotion, and seamless attendee engagement, while providing participants with intuitive discovery and effortless registration. Since our inception, EventFlow has enabled thousands of organizations and individuals to deliver memorable events—ranging from intimate workshops to large-scale international conferences—across a diverse array of industries and communities.
        </p>
        <motion.div
          initial={{ scale: 0.95, opacity: 0 }}
          whileInView={{ scale: 1, opacity: 1 }}
          transition={{ duration: 0.6, delay: 0.2, type: "tween" }}
          viewport={{ once: true }}
          className="bg-coral/10 rounded-lg p-7 max-w-5xl"
        >
          <h2 className="text-2xl font-bold mb-2 text-gray-900">Our Mission</h2>
          <p className="text-gray-800 text-[17px] leading-relaxed">
            Our mission is to simplify and elevate event management for everyone. We strive to foster meaningful human connections by making it easy for anyone to organize, promote, and attend impactful events. Through continuous innovation, a commitment to user experience, and a passion for community, we aim to empower people everywhere to bring their ideas to life and create lasting memories.
          </p>
        </motion.div>
      </motion.section>

      {/* Event Gallery Section */}
      <motion.section
        initial={{ opacity: 0, x: 80 }}
        whileInView={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.7, type: "spring" }}
        viewport={{ once: true }}
        className="max-w-6xl mx-auto py-10 px-4 sm:px-8"
      >
        <h2 className="text-3xl font-extrabold mb-8 text-coral tracking-tight">Event Highlights</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6">
          {eventGallery.map((event) => (
            <motion.div
              key={event.src}
              className="overflow-hidden rounded-2xl shadow-xl bg-white flex flex-col min-h-[370px] max-w-[290px] mx-auto border border-gray-100 hover:shadow-2xl transition-shadow duration-300"
              whileHover={{ scale: 1.045 }}
              transition={{ type: "spring", stiffness: 300 }}
            >
              <img src={event.src} alt={event.title} className="w-full h-40 object-cover rounded-t-2xl" />
              <div className="p-5 flex-1 flex flex-col">
                <h3 className="font-bold text-base mb-2 text-coral leading-snug">{event.title}</h3>
                <p className="text-gray-700 text-[15px] flex-1 leading-relaxed">{event.description}</p>
              </div>
            </motion.div>
          ))}
        </div>
      </motion.section>

      {/* Company Growth Section */}
      <motion.section
        initial={{ opacity: 0, y: 60 }}
        whileInView={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, type: "spring" }}
        viewport={{ once: true }}
        className="max-w-5xl mx-auto py-16 px-4 sm:px-8"
      >
        <h2 className="text-2xl font-bold mb-4 text-coral">How We're Growing</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <motion.div
            className="bg-white border border-coral/30 rounded-lg p-6 shadow-sm"
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1, duration: 0.5, type: "tween" }}
            viewport={{ once: true }}
          >
            <h3 className="text-lg font-semibold mb-2">10,000+ Events Hosted</h3>
            <p className="text-gray-600">From local workshops to international conferences, our platform powers a diverse range of events every year.</p>
          </motion.div>
          <motion.div
            className="bg-white border border-coral/30 rounded-lg p-6 shadow-sm"
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2, duration: 0.5, type: "tween" }}
            viewport={{ once: true }}
          >
            <h3 className="text-lg font-semibold mb-2">Global Reach</h3>
            <p className="text-gray-600">EventFlow is trusted by users in over 30 countries, making event management borderless and easy.</p>
          </motion.div>
          <motion.div
            className="bg-white border border-coral/30 rounded-lg p-6 shadow-sm"
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3, duration: 0.5, type: "tween" }}
            viewport={{ once: true }}
          >
            <h3 className="text-lg font-semibold mb-2">Continuous Innovation</h3>
            <p className="text-gray-600">We constantly update our platform with new features, ensuring organizers and attendees have the best experience possible.</p>
          </motion.div>
        </div>
      </motion.section>

      {/* Platform at a Glance Section */}
      <motion.section
        initial={{ opacity: 0, x: -80 }}
        whileInView={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.7, type: "spring" }}
        viewport={{ once: true }}
        className="max-w-5xl mx-auto py-16 px-4 sm:px-8"
      >
        <h2 className="text-2xl font-bold mb-8 text-coral">Platform at a Glance</h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-8 text-center">
          <motion.div
            className="bg-white border border-coral/20 rounded-lg p-8 shadow-sm flex flex-col items-center"
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1, duration: 0.5, type: 'tween' }}
            viewport={{ once: true }}
          >
            <span className="text-4xl font-bold text-coral mb-2">10,000+</span>
            <span className="text-gray-700 font-medium">Events Hosted</span>
          </motion.div>
          <motion.div
            className="bg-white border border-coral/20 rounded-lg p-8 shadow-sm flex flex-col items-center"
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2, duration: 0.5, type: 'tween' }}
            viewport={{ once: true }}
          >
            <span className="text-4xl font-bold text-coral mb-2">89M</span>
            <span className="text-gray-700 font-medium">Monthly Users</span>
          </motion.div>
          <motion.div
            className="bg-white border border-coral/20 rounded-lg p-8 shadow-sm flex flex-col items-center"
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3, duration: 0.5, type: 'tween' }}
            viewport={{ once: true }}
          >
            <span className="text-4xl font-bold text-coral mb-2">180+</span>
            <span className="text-gray-700 font-medium">Countries Reached</span>
          </motion.div>
        </div>
      </motion.section>

      {/* What We Do Section */}
      <motion.section
        initial={{ opacity: 0, y: 60 }}
        whileInView={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, type: "spring" }}
        viewport={{ once: true }}
        className="max-w-5xl mx-auto py-16 px-4 sm:px-8"
      >
        <h2 className="text-2xl font-bold mb-8 text-coral">What We Do</h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-8 text-center">
          <div className="flex flex-col items-center bg-white border border-coral/20 rounded-lg p-8 shadow-sm">
            <svg className="w-10 h-10 text-coral mb-3" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path d="M12 8v4l3 3"/></svg>
            <span className="font-semibold text-lg mb-1">Event Discovery</span>
            <span className="text-gray-600 text-sm">Find and join events that match your interests, from local meetups to global conferences.</span>
          </div>
          <div className="flex flex-col items-center bg-white border border-coral/20 rounded-lg p-8 shadow-sm">
            <svg className="w-10 h-10 text-coral mb-3" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><path d="M8 12l2 2 4-4"/></svg>
            <span className="font-semibold text-lg mb-1">Seamless Management</span>
            <span className="text-gray-600 text-sm">Organize, promote, and manage your events with powerful, easy-to-use tools.</span>
          </div>
          <div className="flex flex-col items-center bg-white border border-coral/20 rounded-lg p-8 shadow-sm">
            <svg className="w-10 h-10 text-coral mb-3" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path d="M17 9V7a5 5 0 0 0-10 0v2a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2z"/></svg>
            <span className="font-semibold text-lg mb-1">Secure Ticketing</span>
            <span className="text-gray-600 text-sm">Buy and sell tickets with confidence using our secure, reliable platform.</span>
          </div>
        </div>
      </motion.section>
    </div>
  );
}
